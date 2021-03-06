package com.tvd12.ezyhttp.server.core.asm;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import com.tvd12.ezyfox.asm.EzyFunction;
import com.tvd12.ezyfox.asm.EzyFunction.EzyBody;
import com.tvd12.ezyfox.asm.EzyInstruction;
import com.tvd12.ezyfox.reflect.EzyClass;
import com.tvd12.ezyfox.reflect.EzyMethod;
import com.tvd12.ezyfox.reflect.EzyMethods;
import com.tvd12.ezyfox.reflect.EzyReflections;
import com.tvd12.ezyfox.util.EzyLoggable;
import com.tvd12.ezyhttp.server.core.handler.AbstractUncaughtExceptionHandler;
import com.tvd12.ezyhttp.server.core.handler.UncaughtExceptionHandler;
import com.tvd12.ezyhttp.server.core.reflect.ExceptionHandlerMethod;
import com.tvd12.ezyhttp.server.core.reflect.ExceptionHandlerProxy;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtNewMethod;
import lombok.Setter;

public class ExceptionHandlerImplementer extends EzyLoggable {

	@Setter
	private static boolean debug;
	protected final ExceptionHandlerMethod handlerMethod;
	protected final ExceptionHandlerProxy exceptionHandler;
	
	protected final static String PARAMETER_PREFIX = "param";
	protected final static AtomicInteger COUNT = new AtomicInteger(0);
	
	public ExceptionHandlerImplementer(
			ExceptionHandlerProxy exceptionHandler, ExceptionHandlerMethod handlerMethod) {
		this.handlerMethod = handlerMethod;
		this.exceptionHandler = exceptionHandler;
	}
	
	public UncaughtExceptionHandler implement() {
		try {
			return doimplement();
		}
		catch(Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("rawtypes")
	protected UncaughtExceptionHandler doimplement() throws Exception {
		ClassPool pool = ClassPool.getDefault();
		String implClassName = getImplClassName();
		CtClass implClass = pool.makeClass(implClassName);
		EzyClass superClass = new EzyClass(getSuperClass());
		String exceptionHandlerFieldContent = makeExceptionHandlerFieldContent();
		String setExceptionHandlerMethodContent = makeSetExceptionHandlerMethodContent();
		String handleExceptionMethodContent = makeHandleExceptionMethodContent();
		String getResponseContentTypeMethodContent = makeGetResponseContentTypeMethodContent();
		printComponentContent(implClassName);
		printComponentContent(exceptionHandlerFieldContent);
		printComponentContent(setExceptionHandlerMethodContent);
		printComponentContent(handleExceptionMethodContent);
		printComponentContent(getResponseContentTypeMethodContent);
		implClass.setSuperclass(pool.get(superClass.getName()));
		implClass.addField(CtField.make(exceptionHandlerFieldContent, implClass));
		implClass.addMethod(CtNewMethod.make(setExceptionHandlerMethodContent, implClass));
		implClass.addMethod(CtNewMethod.make(handleExceptionMethodContent, implClass));
		implClass.addMethod(CtNewMethod.make(getResponseContentTypeMethodContent, implClass));
		Class answerClass = implClass.toClass();
		implClass.detach();
		UncaughtExceptionHandler handler = (UncaughtExceptionHandler) answerClass.newInstance();
		setRepoComponent(handler);
		return handler;
	}
	
	protected void setRepoComponent(UncaughtExceptionHandler handler) {
		handler.setExceptionHandler(exceptionHandler.getInstance());
	}
	
	protected String makeExceptionHandlerFieldContent() {
		return new EzyInstruction()
				.append("private ")
					.append(exceptionHandler.getClazz().getName())
						.append(" exceptionHandler")
				.toString();
	}
	
	protected String makeSetExceptionHandlerMethodContent() {
		return new EzyFunction(getSetExceptionHandlerMethod())
				.body()
					.append(new EzyInstruction("\t", "\n")
							.append("this.exceptionHandler")
							.equal()
							.brackets(exceptionHandler.getClazz().getClazz())
							.append("arg0"))
					.function()
				.toString();
	}
	
	protected String makeHandleExceptionMethodContent() {
		EzyMethod method = getHandleExceptionMethod();
		EzyFunction function = new EzyFunction(method);
		EzyBody body = function.body();
		for(Class<?> exceptionClass : handlerMethod.getExceptionClasses()) {
			EzyInstruction instructionIf = new EzyInstruction("\t", "\n", false)
					.append("if(arg0 instanceof ")
						.append(exceptionClass.getName())
					.append(") {");
			body.append(instructionIf);
			EzyInstruction instructionHandle = new EzyInstruction("\t\t", "\n");
			Class<?> returnType = handlerMethod.getReturnType();
			if(returnType != void.class)
				instructionHandle.answer();
			instructionHandle
					.append("this.exceptionHandler.").append(handlerMethod.getName())
					.bracketopen()
						.brackets(exceptionClass).append("arg0")
					.bracketclose();
			body.append(instructionHandle);
			if(returnType == void.class)
				body.append(new EzyInstruction("\t\t", "\n").append("return null"));
			body.append(new EzyInstruction("\t", "\n", false).append("}"));
		}
		body.append(new EzyInstruction("\t", "\n").append("throw arg0"));
		return toThrowExceptionFunction(method, function);
	}
	
	protected String makeGetResponseContentTypeMethodContent() {
		return new EzyFunction(getGetResponseContentTypeMethod())
				.body()
					.append(new EzyInstruction("\t", "\n")
							.answer()
							.string(handlerMethod.getResponseType()))
					.function()
				.toString();
	}
	
	protected String toThrowExceptionFunction(EzyMethod method, EzyFunction function) {
		return new StringBuilder()
				.append(method.getDeclaration(EzyReflections.MODIFIER_PUBLIC))
				.append(" throws Exception {\n")
				.append(function.body())
				.append("}")
				.toString();
	}
	
	protected EzyMethod getSetExceptionHandlerMethod() {
		Method method = EzyMethods.getMethod(
				AbstractUncaughtExceptionHandler.class, "setExceptionHandler", Object.class);
		return new EzyMethod(method);
	}
	
	protected EzyMethod getHandleExceptionMethod() {
		Method method = EzyMethods.getMethod(
				AbstractUncaughtExceptionHandler.class, "handleException", Exception.class);
		return new EzyMethod(method);
	}
	
	protected EzyMethod getGetResponseContentTypeMethod() {
		Method method = EzyMethods.getMethod(
				AbstractUncaughtExceptionHandler.class, "getResponseContentType");
		return new EzyMethod(method);
	}
	
	protected Class<?> getSuperClass() {
		return AbstractUncaughtExceptionHandler.class;
	}
	
	protected String getImplClassName() {
		return exceptionHandler.getClassSimpleName()
				+ "$" + handlerMethod.getName() + "$ExceptionHandler$AutoImpl$" + COUNT.incrementAndGet();
	}
	
	protected void printComponentContent(String componentContent) {
		if(debug) 
			logger.debug("component content: \n{}", componentContent);
	}
	
}
