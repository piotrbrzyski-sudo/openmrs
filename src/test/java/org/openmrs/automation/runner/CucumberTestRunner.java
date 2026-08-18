package org.openmrs.automation.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

@CucumberOptions(
        features = "classpath:features",
        glue = "org.openmrs.automation",
        plugin = {
            "pretty",
            "html:target/cucumber-reports/cucumber.html",
            "json:target/cucumber-reports/cucumber.json",
            "junit:target/cucumber-reports/cucumber.xml",
            "rerun:target/cucumber-reports/rerun.txt"
        },
        monochrome = true)
public final class CucumberTestRunner extends AbstractTestNGCucumberTests {
    @Override
    @DataProvider(parallel = false)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
