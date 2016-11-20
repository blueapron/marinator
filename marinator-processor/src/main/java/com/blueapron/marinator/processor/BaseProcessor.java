package com.blueapron.marinator.processor;

import com.squareup.javapoet.JavaFile;

import java.io.IOException;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * Helper class to write processors.
 */
public abstract class BaseProcessor extends AbstractProcessor {

    protected Messager mMessager;
    protected Filer mFiler;
    protected Types mTypeUtils;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        mMessager = env.getMessager();
        mFiler = env.getFiler();
        mTypeUtils = env.getTypeUtils();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    protected void writeFile(JavaFile file, String errorMsg) {
        writeFile(file, errorMsg, null /* elt */);
    }

    protected void writeFile(JavaFile file, String errorMsg, Element elt) {
        try {
            file.writeTo(mFiler);
        } catch (IOException ioe) {
            mMessager.printMessage(Diagnostic.Kind.ERROR, errorMsg, elt);
        }
    }
}
