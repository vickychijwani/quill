package me.vickychijwani.spectre.testing;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.concurrent.Callable;

import io.reactivex.Scheduler;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.functions.Function;
import io.reactivex.internal.schedulers.TrampolineScheduler;
import io.reactivex.plugins.RxJavaPlugins;

public class RxSchedulersRule implements TestRule {

    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Scheduler scheduler = TrampolineScheduler.instance();
                Function<Callable<Scheduler>, Scheduler> schedulerFn = __ -> scheduler;
                RxJavaPlugins.reset();
                RxJavaPlugins.setInitIoSchedulerHandler(schedulerFn);
                RxJavaPlugins.setInitNewThreadSchedulerHandler(schedulerFn);
                RxAndroidPlugins.reset();
                RxAndroidPlugins.setInitMainThreadSchedulerHandler(schedulerFn);

                try {
                    base.evaluate();
                } finally {
                    RxJavaPlugins.reset();
                    RxAndroidPlugins.reset();
                }
            }
        };
    }

}
