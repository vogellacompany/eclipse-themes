/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.vogella.eclipse.themes.launcher;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.Label;
import java.lang.classfile.MethodModel;
import java.lang.classfile.MethodTransform;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;

import org.eclipse.core.runtime.ILog;
import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;

/**
 * Prepends a call to {@link StyledWorkspaceLauncher#show} to {@code IDEApplication.showChooseWorkspaceDialog}, so
 * the classic dialog still opens whenever that call returns false.
 */
class IDEApplicationWeaver implements WeavingHook {

	static final String TARGET_CLASS = "org.eclipse.ui.internal.ide.application.IDEApplication";
	private static final String TARGET_METHOD = "showChooseWorkspaceDialog";

	private static final ClassDesc SHELL = ClassDesc.of("org.eclipse.swt.widgets.Shell");
	private static final ClassDesc LAUNCH_DATA = ClassDesc.of("org.eclipse.ui.internal.ide.ChooseWorkspaceData");
	private static final MethodTypeDesc TARGET_TYPE = MethodTypeDesc.of(ConstantDescs.CD_void, SHELL, LAUNCH_DATA,
			ConstantDescs.CD_boolean);
	private static final ClassDesc LAUNCHER = ClassDesc.of(StyledWorkspaceLauncher.class.getName());
	private static final MethodTypeDesc SHOW_TYPE = MethodTypeDesc.of(ConstantDescs.CD_boolean, SHELL, LAUNCH_DATA,
			ConstantDescs.CD_boolean);

	private final Bundle bundle;
	private volatile boolean done;

	IDEApplicationWeaver(Bundle bundle) {
		this.bundle = bundle;
	}

	@Override
	public void weave(WovenClass wovenClass) {
		if (done || !TARGET_CLASS.equals(wovenClass.getClassName())) {
			return;
		}
		done = true;
		try {
			byte[] woven = weave(wovenClass.getBytes());
			if (woven == null) {
				ILog.of(bundle).warn(TARGET_METHOD + " not found in " + TARGET_CLASS
						+ ", the classic workspace dialog stays in place");
				return;
			}
			wovenClass.setBytes(woven);
			wovenClass.getDynamicImports().add(StyledWorkspaceLauncher.class.getPackageName());
		} catch (RuntimeException | LinkageError e) {
			ILog.of(bundle).error("Could not weave " + TARGET_CLASS + ", the classic workspace dialog stays in place", e);
		}
	}

	static byte[] weave(byte[] bytes) {
		ClassFile classFile = ClassFile.of();
		ClassModel model = classFile.parse(bytes);
		boolean present = model.methods().stream().anyMatch(IDEApplicationWeaver::isTarget);
		if (!present) {
			return null;
		}
		ClassTransform transform = ClassTransform.transformingMethods(IDEApplicationWeaver::isTarget,
				MethodTransform.transformingCode(new CodeTransform() {
					@Override
					public void atStart(CodeBuilder code) {
						Label classic = code.newLabel();
						code.aload(1).aload(2).iload(3).invokestatic(LAUNCHER, "show", SHOW_TYPE).ifeq(classic).return_()
								.labelBinding(classic);
					}

					@Override
					public void accept(CodeBuilder code, CodeElement element) {
						code.with(element);
					}
				}));
		return classFile.transformClass(model, transform);
	}

	private static boolean isTarget(MethodModel method) {
		return method.methodName().equalsString(TARGET_METHOD)
				&& method.methodTypeSymbol().equals(TARGET_TYPE);
	}
}
