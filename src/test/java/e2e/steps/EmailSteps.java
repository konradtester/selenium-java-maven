package e2e.steps;

import e2e.EnvConfig;
import e2e.TestContext;
import io.cucumber.java.en.Then;
import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.RecipientStringTerm;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailSteps {

    private final TestContext context;

    public EmailSteps(TestContext context) {
        this.context = context;
    }

    @Then("I wait for email sent to {string} containing {string}")
    public void waitForEmailContaining(String recipientEmail, String text) {
        String resolvedEmail = context.resolve(recipientEmail);
        String resolvedText = context.resolve(text);
        fetchEmailBody(resolvedEmail, resolvedText, 30);
    }

    @Then("I save the link matching {string} from email sent to {string} as variable {string}")
    public void saveLinkFromEmail(String linkPattern, String recipientEmail, String varName) {
        String resolvedEmail = context.resolve(recipientEmail);
        String resolvedPattern = context.resolve(linkPattern);

        String body = fetchEmailBody(resolvedEmail, resolvedPattern, 30);

        // Extract all URLs via regex (either href="..." attributes or bare https?:// links)
        Pattern urlPattern = Pattern.compile("href=\"([^\"]+)\"|https?://[^\\s<>\"]+");
        Matcher matcher = urlPattern.matcher(body);
        String matchingUrl = null;
        while (matcher.find()) {
            String url = matcher.group(1) != null ? matcher.group(1) : matcher.group();
            if (url.contains(resolvedPattern)) {
                matchingUrl = url;
                break;
            }
        }

        if (matchingUrl == null) {
            throw new IllegalStateException("No link matching pattern '" + resolvedPattern + "' was found in the email.");
        }

        context.getState().put(varName, matchingUrl.trim());
    }

    /**
     * Fetches the body of an email sent to the given recipient via IMAP.
     * Polls until the message arrives or the timeout is reached.
     */
    private String fetchEmailBody(String recipientEmail, String containsText, int timeoutSeconds) {
        String imapHost = EnvConfig.get("IMAP_HOST");
        String imapUser = EnvConfig.get("IMAP_USER");
        String imapPassword = EnvConfig.get("IMAP_PASSWORD");

        if (isBlank(imapHost) || isBlank(imapUser) || isBlank(imapPassword)) {
            throw new IllegalStateException(
                    "Missing mailbox configuration in .env (required: IMAP_HOST, IMAP_USER, IMAP_PASSWORD)."
            );
        }

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");

        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;

        try {
            Session session = Session.getInstance(props);
            Store store = session.getStore("imaps");
            try {
                store.connect(imapHost, imapUser, imapPassword);
                Folder inbox = store.getFolder("INBOX");
                inbox.open(Folder.READ_ONLY);
                try {
                    while (System.currentTimeMillis() < deadline) {
                        RecipientStringTerm term = new RecipientStringTerm(Message.RecipientType.TO, recipientEmail);
                        Message[] messages = inbox.search(term);

                        // Check the newest first
                        for (int i = messages.length - 1; i >= 0; i--) {
                            Message message = messages[i];
                            String subject = message.getSubject() == null ? "" : message.getSubject();
                            String body = extractBody(message);
                            if (containsText == null || body.contains(containsText) || subject.contains(containsText)) {
                                return body;
                            }
                        }
                        Thread.sleep(2000);
                    }
                    throw new IllegalStateException(String.format(
                            "No message sent to %s containing '%s' was found within %ds.",
                            recipientEmail, containsText, timeoutSeconds
                    ));
                } finally {
                    inbox.close(false);
                }
            } finally {
                store.close();
            }
        } catch (MessagingException | IOException e) {
            throw new RuntimeException("Failed to fetch email: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private String extractBody(Message message) throws MessagingException, IOException {
        Object content = message.getContent();
        if (content instanceof MimeMultipart multipart) {
            StringBuilder sb = new StringBuilder();
            appendMultipart(multipart, sb);
            return sb.toString();
        }
        return content == null ? "" : content.toString();
    }

    private void appendMultipart(MimeMultipart multipart, StringBuilder sb) throws MessagingException, IOException {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            String disposition = part.getDisposition();
            if (disposition != null && disposition.equalsIgnoreCase(Part.ATTACHMENT)) {
                continue;
            }

            Object content = part.getContent();
            if (content instanceof MimeMultipart nested) {
                appendMultipart(nested, sb);
            } else if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
                sb.append(content.toString());
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }
}
