package me.vickychijwani.spectre.testing;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import timber.log.Timber;

public class LoggingRule implements TestRule {

    private final Timber.Tree tree = new Timber.Tree() {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (message != null) {
                System.out.println(message);
            }
            if (t != null) {
                t.printStackTrace();
            }
        }
    };

    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Timber.plant(tree);
                base.evaluate();
            }
        };
    }

}
