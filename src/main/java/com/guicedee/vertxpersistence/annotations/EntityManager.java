package com.guicedee.vertxpersistence.annotations;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.*;

/**
 * Annotation to identify entity managers at package, class, method, and field levels.
 * When applied, it indicates which entity manager should be used for the annotated element.
 * If no entity manager annotation is found, the default package entity manager is used.
 */
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@BindingAnnotation
public @interface EntityManager
{
    /**
     * The name of the entity manager. If not specified, the default package name is used.
     *
     * @return The entity manager name
     */
    String value() default "";

    /**
     * Whether the entity classes should be included in the entity manager. If false, only that package classes are loaded
     *
     * @return True if all classes should be included, false otherwise. Default is true.
     */
    boolean allClasses() default true;

    /**
     * Indicates whether this entity manager should be used as the default when no specific
     * entity manager is specified. If multiple entity managers are marked as default,
     * the first one found will be used.
     *
     * @return True if this is the default entity manager, false otherwise
     */
    boolean defaultEm() default true;
}
