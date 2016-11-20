package com.blueapron.marinator.processor;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;

/**
 * Helper functions to handle common functionality.
 */
public final class Utils {

    private Utils() {
    }

    public static String getPackageName(TypeElement type) {
        while (true) {
            Element enclosing = type.getEnclosingElement();
            if (enclosing instanceof PackageElement) {
                return ((PackageElement) enclosing).getQualifiedName().toString();
            }
            type = (TypeElement) enclosing;
        }
    }

    public static List<VariableElement> getNonPrivateVariables(TypeElement type) {
        List<VariableElement> results = new ArrayList<>();
        for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
            if (!field.getModifiers().contains(Modifier.PRIVATE)) {
                results.add(field);
            }
        }
        return results;
    }
}
