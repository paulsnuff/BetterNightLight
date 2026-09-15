DEV_KEYSTORE ?= app/dev-release.keystore
KEYTOOL ?= $(shell command -v keytool 2>/dev/null || for d in "$$JAVA_HOME/bin/keytool" "$$HOME/.local/share/JetBrains/Toolbox/apps/android-studio/jbr/bin/keytool" /opt/android-studio/jbr/bin/keytool /usr/local/android-studio/jbr/bin/keytool /snap/android-studio/current/jbr/bin/keytool "$$HOME"/.gradle/jdks/*/bin/keytool /usr/lib/jvm/*/bin/keytool; do [ -x "$$d" ] && echo "$$d" && break; done)

.PHONY: keystore
keystore:
	if [ -z "$(KEYTOOL)" ]; then \
		echo "Error: keytool not found. Install a JDK or set JAVA_HOME."; \
		exit 1; \
	fi; \
	if [ -f "$(DEV_KEYSTORE)" ]; then \
		echo "Dev keystore already exists: $(DEV_KEYSTORE)"; \
		echo "Delete it first if you want to regenerate it."; \
	else \
		$(KEYTOOL) -genkeypair \
			-keystore $(DEV_KEYSTORE) \
			-alias dev \
			-keyalg RSA -keysize 4096 -validity 10000 \
			-storepass betternightlight -keypass betternightlight \
			-dname "CN=BetterNightLight Dev, O=Dev, C=PL" \
		&& echo "Dev keystore generated: $(DEV_KEYSTORE)" \
		&& echo "Local release builds are now signed with it automatically."; \
	fi
