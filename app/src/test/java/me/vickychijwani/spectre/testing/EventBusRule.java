package me.vickychijwani.spectre.testing;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import me.vickychijwani.spectre.event.BusProvider;

public class EventBusRule implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                BusProvider.setupForTesting();
                base.evaluate();
            }
        };
    }

}
