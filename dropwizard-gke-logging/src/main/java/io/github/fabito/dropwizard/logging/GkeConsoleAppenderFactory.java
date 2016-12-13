package io.github.fabito.dropwizard.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.jackson.JacksonJsonFormatter;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.ConsoleAppenderFactory;
import io.dropwizard.logging.async.AsyncAppenderFactory;
import io.dropwizard.logging.filter.FilterFactory;
import io.dropwizard.logging.filter.LevelFilterFactory;
import io.dropwizard.logging.layout.LayoutFactory;

import java.util.Map;

/**
 * {@link ConsoleAppenderFactory} extension which uses a Stackdriver compatible {@link JsonLayout}
 *
 * It outputs a json per line Stackdriver [LogEntry](https://cloud.google.com/logging/docs/view/logs_index)
 *
 *
 */
@JsonTypeName("GKEConsole")
public class GkeConsoleAppenderFactory<E extends DeferredProcessingAware> extends ConsoleAppenderFactory<E> {

    @Override
    public Appender<E> build(LoggerContext context, String applicationName, LayoutFactory<E> layoutFactory, LevelFilterFactory<E> levelFilterFactory, AsyncAppenderFactory<E> asyncAppenderFactory) {
        ConsoleAppender appender = new ConsoleAppender();
        appender.setName("gke-console");
        appender.setContext(context);
        appender.setTarget(getTarget().get());

        LayoutWrappingEncoder layoutEncoder = new LayoutWrappingEncoder();
        JsonLayout jsonLayout = new StackdriverLoggingJsonLayout(context);
        jsonLayout.start();

        layoutEncoder.setLayout(jsonLayout);
        appender.setEncoder(layoutEncoder);
        appender.addFilter(levelFilterFactory.build(this.threshold));

        for(FilterFactory ff : this.getFilterFactories()) {
            appender.addFilter(ff.build());
        }

        appender.start();
        return this.wrapAsync(appender, asyncAppenderFactory);
    }


    private static class StackdriverLoggingJsonLayout extends JsonLayout {

        StackdriverLoggingJsonLayout(LoggerContext context) {
            super();
            this.includeLevel = false;
            this.setAppendLineSeparator(true);
            this.setJsonFormatter(new JacksonJsonFormatter());
            this.setContext(context);
        }

        @Override
        protected void addCustomDataToJsonMap(Map<String, Object> map, ILoggingEvent event) {
            map.put("severity", String.valueOf(event.getLevel()));
        }
    }

}