package utils;


import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReportUtils {

    private static ExtentReports extent;
    private static ExtentTest test;

    public static void initReports() {
        if (extent == null) {
            ExtentSparkReporter reporter = new ExtentSparkReporter("reports/ExtentReports/extent-report.html");
            extent = new ExtentReports();
            extent.attachReporter(reporter);
        }
    }

    public static void createTest(String testName) {
        test = extent.createTest(testName);
    }

    public static void logInfo(String message) {
        if (test != null) test.info(message);
    }

    public static void logPass(String message) {
        if (test != null) test.pass(message);
    }

    public static void logFail(String message) {
        if (test != null) test.fail(message);
    }

    // ✅ Capture Screenshot
    public static void captureScreenshot(WebDriver driver, String stepName) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String path = "reports/Screenshots/" + stepName + "_" + timestamp + ".png";

            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File dest = new File(path);
            Files.createDirectories(dest.getParentFile().toPath());
            Files.copy(src.toPath(), dest.toPath());

            if (test != null) {
                test.addScreenCaptureFromPath(path, stepName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void flushReports() {
        if (extent != null) extent.flush();
    }
}


