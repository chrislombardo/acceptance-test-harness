package org.jenkinsci.test.acceptance.po;

import com.google.inject.Injector;
import org.openqa.selenium.By;

/**
 * Special kind of page object that maps to a portion of a page with multiple INPUT controls.
 *
 * <p>
 * Typically we use this to map a set of controls in the configuration page, which is generated
 * by composing various config.jelly files from different extension points.
 *
 * @author Oliver Gondza
 */
public abstract class PageArea extends CapybaraPortingLayer {
    /**
     * Element path that points to this page area.
     */
    public final String path;

    protected PageArea(Injector injector, String path) {
        super(injector);
        this.path = path;
    }

    protected PageArea(PageObject context, String path) {
        super(context.injector);
        this.path = path;
    }

    /**
     * Returns the "path" selector that finds an element by following the form-element-path plugin.
     *
     * https://wiki.jenkins-ci.org/display/JENKINS/Form+Element+Path+Plugin
     */
    public By path(String rel) {
        return by.path(path + '/' + rel);
    }

    /**
     * Create a control object that wraps access to the specific INPUT element in this page area.
     *
     * The {@link Control} object itself can be created early as the actual element resolution happens
     * lazily. This means {@link PageArea} implementations can put these in their fields.
     *
     * Several paths can be provided to find the first matching element. Useful
     * when element path changed between versions.
     */
    public Control control(String... relativePaths) {
        return new Control(this,relativePaths);
    }
}
