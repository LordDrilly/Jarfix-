#include <sys/wait.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <jni.h>
#include "javaloader.h"

JNIEnv* create_vm(JavaVM **jvm) {
	JNIEnv *env;
	JavaVMInitArgs args;
	JavaVMOption options[1];
	args.version = JNI_VERSION_1_6;
	args.nOptions = 1;
	options[0].optionString = JAVA_CLASSPATH;
	args.options = options;
	args.ignoreUnrecognized = JNI_FALSE;

	JNI_CreateJavaVM(jvm, (void **)&env, &args);
	return env;
}

int main(int argc, char *argv[]) {
	JavaVM *jvm = NULL;
	JNIEnv *env = create_vm(&jvm);
	jclass helloWorldClass;
	jmethodID mainMethod;
	jclass waitClass;
	jmethodID waitMethod;
	jobjectArray applicationArgs;
	jstring applicationArg;
	int cnt;
#ifdef USE_FORKING_METHOD
	pid_t cpid;
#endif

	helloWorldClass = (*env)->FindClass(env, JAVA_MAIN_CLASS);
	waitClass = (*env)->FindClass(env, "com/granolamatt/jarutils/GuiWaitHelper");
	mainMethod = (*env)->GetStaticMethodID(env, helloWorldClass, "main", "([Ljava/lang/String;)V");
	waitMethod = (*env)->GetStaticMethodID(env, waitClass, "waitForGui", "()V");
	applicationArgs = (*env)->NewObjectArray(env, argc, (*env)->FindClass(env, "java/lang/String"), NULL);
	
	for (cnt = 0; cnt < argc; cnt++) {
		applicationArg = (*env)->NewStringUTF(env, argv[cnt]);
		(*env)->SetObjectArrayElement(env, applicationArgs, cnt, applicationArg);
	}

#ifdef USE_FORKING_METHOD
	cpid = fork();
	if (cpid == -1) { perror("fork"); exit(EXIT_FAILURE); }
	if (cpid == 0) { /* Child */
		(*env)->CallStaticVoidMethod(env, helloWorldClass, mainMethod, applicationArgs);
		(*env)->CallStaticVoidMethod(env, waitClass, waitMethod);
	} else {		/* Parent */
		wait(NULL);	/* Wait for child */
		//exit(EXIT_SUCCESS);
	}
#else
	(*env)->CallStaticVoidMethod(env, helloWorldClass, mainMethod, applicationArgs);
	(*env)->CallStaticVoidMethod(env, waitClass, waitMethod);
#endif

	printf("CLEAN EXIT\n");
    fflush(stdout);
	return 0;
}

