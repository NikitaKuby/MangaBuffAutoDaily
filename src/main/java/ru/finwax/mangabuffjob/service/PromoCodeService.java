package ru.finwax.mangabuffjob.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.finwax.mangabuffjob.Entity.UserCookie;
import ru.finwax.mangabuffjob.repository.UserCookieRepository;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.time.Duration;
import ru.finwax.mangabuffjob.auth.MbAuth;
import java.util.function.BiConsumer;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.NoSuchElementException;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromoCodeService {

    private final UserCookieRepository userCookieRepository;
    private final AccountService accountService;
    private final TaskExecutor taskExecutor;
    private final MbAuth mbAuth;
    private final DriverManager driverManager;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private AtomicBoolean stopProcessing = new AtomicBoolean(false);

    public void applyPromoCodeToAllAccounts(String promoCode, BiConsumer<String, String> notificationCallback) {
        List<UserCookie> accounts = accountService.getAllAccountsInOrder();

        List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();

        for (UserCookie account : accounts) {
            if (stopProcessing.get()) {
                log.info("Promo code processing stopped by request.");
                notificationCallback.accept("Promo code processing stopped.", "info");
                break; // Exit the loop if stopProcessing is true
            }
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    applyPromoCodeToAccount(account, promoCode, notificationCallback);
                } catch (Exception e) {
                    log.error("Error processing account " + account.getId() + ": " + e.getMessage()); // Keep for debugging during dev
                    // TODO: Show error notification for this account
                    notificationCallback.accept("Error processing account " + account.getUsername() + ": " + e.getMessage(), "error");
                }
            }, executorService);
            futures.add(future);
        }

        // Optionally, wait for all futures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("All promo code application tasks completed.");
        notificationCallback.accept("All promo code application tasks completed.", "success");
    }

    private void applyPromoCodeToAccount(UserCookie account, String promoCode, BiConsumer<String, String> notificationCallback) {
        ChromeDriver driver = null;
        try {
            String driverId = driverManager.generateDriverId(account.getId(), "promoCode");
            // Initialize WebDriver using MbAuth
            driver = mbAuth.getActualDriver(account.getId(), "promoCode", false); // Use a specific task name and hide views

            driver.get("https://mangabuff.ru/promo-code");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // Find and input promo code
            By promoCodeInputLocator = By.cssSelector("input.form__field.promo-code-use-input");
            try {
                wait.until(ExpectedConditions.visibilityOfElementLocated(promoCodeInputLocator)).sendKeys(promoCode);
            } catch (TimeoutException | NoSuchElementException e) {
                notificationCallback.accept("[" + account.getUsername() + "] Error: Promo code input field not found. Page structure might have changed.", "error");
                return; // Stop processing for this account
            }

            // Find and click apply button
            By applyButtonLocator = By.cssSelector("button.button.button--primary.promo-code-use-btn");
            try {
                wait.until(ExpectedConditions.elementToBeClickable(applyButtonLocator)).click();
            } catch (TimeoutException | NoSuchElementException e) {
                notificationCallback.accept("[" + account.getUsername() + "] Error: Apply promo code button not found. Page structure might have changed.", "error");
                return; // Stop processing for this account
            }

            monitorToastNotifications(driver, account.getId(), account.getUsername(), notificationCallback);

        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            notificationCallback.accept("[" + account.getUsername() + "] Authorization error. Re-login required.", "error");
        } catch (org.springframework.web.client.HttpServerErrorException.BadGateway e) {
            notificationCallback.accept("[" + account.getUsername() + "] Error 502 Bad Gateway: Site unavailable.", "error");
        } catch (Exception e) {
            log.error("Unexpected error applying promo code for account " + account.getId() + ": " + e.getMessage()); // Keep for debugging during dev
            notificationCallback.accept("Error applying promo code for account " + account.getUsername() + ": " + e.getMessage(), "error");
        } finally {
            if (driver != null) {
                driverManager.unregisterDriver(account.getId().toString());
            }
        }
    }

    // TODO: Implement toast notification monitoring logic
    private void monitorToastNotifications(WebDriver driver, Long accountId, String username, BiConsumer<String, String> notificationCallback) {
        // This method will need to actively look for the toast elements
        // and interpret their content and type.
        // You'll likely need a loop with a wait condition or similar.

        long endTime = System.currentTimeMillis() + 5000; // Monitor for 5 seconds

        while (System.currentTimeMillis() < endTime) {
            try {
                // Find all toast containers
                List<org.openqa.selenium.WebElement> toastContainers = driver.findElements(By.id("toast-container"));

                for (org.openqa.selenium.WebElement container : toastContainers) {
                    // Find all toasts within the container
                    List<org.openqa.selenium.WebElement> toasts = container.findElements(By.className("toast"));

                    for (org.openqa.selenium.WebElement toast : toasts) {
                        String toastClass = toast.getAttribute("class");
                        String toastMessage = "";
                        try {
                             toastMessage = toast.findElement(By.className("toast-message")).getText();
                        } catch (org.openqa.selenium.NoSuchElementException e) {
                            // Ignore toasts without a message
                            continue;
                        }

                        log.info("[" + accountId + "] Detected notification: Class = " + toastClass + ", Message = " + toastMessage);

                        if (toastClass.contains("toast-error")) {
                            if (toastMessage.contains("Промокод не найден.") || toastMessage.contains("Промокод больше не доступен.")) {
                                notificationCallback.accept("[" + username + "] Critical Error: " + toastMessage + ". Stopping processing.", "error");
                                // TODO: Signal to stop processing for other accounts
                                // This might require a shared flag or a different approach to manage the futures.
                                stopProcessing.set(true); // Set the flag to stop other tasks
                                return; // Stop monitoring for this account
                            } else {
                                notificationCallback.accept("[" + username + "] Error: " + toastMessage, "error");
                                // TODO: Show red border notification in UI
                            }
                        } else {
                            notificationCallback.accept("[" + username + "] Notification: " + toastMessage, "success");
                            // TODO: Show green border notification in UI
                        }
                    }
                }
                // Small wait to avoid busy-looping
                Thread.sleep(200);
            } catch (org.openqa.selenium.WebDriverException e) {
                 // Handle potential StaleElementReferenceException or other driver errors during monitoring
                 log.info("[" + accountId + "] Error monitoring notifications: " + e.getMessage());
                 break; // Exit monitoring loop for this account on error
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                notificationCallback.accept("[" + username + "] Notification monitoring interrupted.", "error");
                break; // Exit monitoring loop if interrupted
            }
        }
    }

    // TODO: Implement UI notification display logic
    private void showNotification(String message, String type) {
        // This method will be called from the controller or a dedicated notification service
        // to display the toast messages in the UI.
    }
} 