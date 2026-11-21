.DEFAULT_GOAL := help

GRADLE := ./gradlew
WEB := web
BUNDLES := $(WEB)/public/flagon

ARGS ?=
MINECRAFT ?= 1.8.9
LAUNCHER ?=
N ?= 3
ENGINES ?= chromium firefox

VENDOR_JARS := $(wildcard vendor/*.jar)


.PHONY: help setup deps browsers fast bundles app verify-bundles \
        minecraft bundles-release size test browser flake typecheck lint build-site \
        deploy-private dev serve check report merge-reports watch version release publish clean

help: ## show available commands
	@grep -hE '^[a-z-]+:.*## ' $(MAKEFILE_LIST) \
		| awk 'BEGIN { FS = ":.*## " }; { printf "  %-20s %s\n", $$1, $$2 }'

setup: deps browsers ## install project dependencies and browser test engines

deps: ## install web dependencies
	cd $(WEB) && bun install --frozen-lockfile

browsers: ## install Playwright engines and their system dependencies (ENGINES="chromium firefox")
	cd $(WEB) && bunx playwright install-deps $(ENGINES)
	cd $(WEB) && bunx playwright install $(ENGINES)

fast: ## compile the fast development bundles
	$(GRADLE) bundlesFast
	@$(MAKE) verify-bundles
	@echo "Fast bundles built. Run 'make bundles' for the full build."

bundles: ## compile Java to WebAssembly and JavaScript
	$(GRADLE) bundles
	@$(MAKE) verify-bundles

app: ## build the JARs in vendor/ as runnable app bundles
	@test -n "$(VENDOR_JARS)" || { \
		echo "no JARs found in vendor/"; \
		exit 1; \
	}
	$(GRADLE) bundles
	@$(MAKE) verify-bundles
	@echo "App bundles are available in $(BUNDLES)/"
	@echo "Run 'make dev' and open http://localhost:5273/"

minecraft: ## fill vendor/ from a local launcher install (MINECRAFT=1.8.9 LAUNCHER=path)
	cd $(WEB) && bun vendor-minecraft.ts $(MINECRAFT) $(LAUNCHER)

verify-bundles: ## verify generated bundles contain compiled output
	@for bundle in $(BUNDLES)/js/*.js $(BUNDLES)/wasm/*.wasm $(BUNDLES)/jspi/*.wasm; do \
		test -f "$$bundle" || continue; \
		size=$$(wc -c < "$$bundle" | tr -d " "); \
		test $$size -ge 20000 || { \
			echo "$$bundle is only $$size bytes; run 'make clean' and rebuild"; \
			exit 1; \
		}; \
		if grep -q "Unresolved compilation problem" "$$bundle"; then \
			echo "$$bundle contains an unresolved compilation error"; \
			exit 1; \
		fi; \
	done
	@for catalog in $(BUNDLES)/*.catalog.json; do \
		test -f "$$catalog" || continue; \
		grep -q '"id"' "$$catalog" || { \
			echo "$$catalog contains no programs"; \
			exit 1; \
		}; \
	done

bundles-release: ## build release bundles and check their size budgets
	$(GRADLE) -Pflagon.release bundles
	@$(MAKE) verify-bundles
	@$(MAKE) size

size: ## check release bundles against their size budgets
	@over=0; \
	printf "%-16s %10s %10s %8s\n" bundle bytes budget left; \
	while read -r name limit; do \
		case $$name in \#*|"") continue ;; esac; \
		file=$(BUNDLES)/$$name; \
		test -f "$$file" || { \
			echo "$$name was not built"; \
			exit 1; \
		}; \
		size=$$(wc -c < "$$file" | tr -d " "); \
		left=$$((limit - size)); \
		printf "%-16s %10s %10s %8s\n" "$$name" "$$size" "$$limit" "$$left"; \
		test $$size -le $$limit || over=1; \
	done < toolchain/size-budget.txt; \
	test $$over -eq 0 || { \
		echo "one or more bundles exceed their size budget"; \
		exit 1; \
	}

test: ## run JVM unit tests
	$(GRADLE) test

browser: ## run Playwright browser tests
	@test -d $(BUNDLES) || { \
		echo "bundles not found; run 'make bundles' first"; \
		exit 1; \
	}
	cd $(WEB) && bunx playwright test $(ARGS)

flake: ## repeat Playwright tests to detect intermittent failures
	@test -d $(BUNDLES) || { \
		echo "bundles not found; run 'make bundles' first"; \
		exit 1; \
	}
	cd $(WEB) && bunx playwright test --repeat-each=$(N) $(ARGS)

typecheck: ## type-check the web application
	cd $(WEB) && bun run typecheck

lint: ## run Java and web linters
	$(GRADLE) lintJava
	cd $(WEB) && bun run lint

build-site: ## build the web application for deployment
	cd $(WEB) && bun run build

deploy-private: ## deploy the site and app bundle behind Vercel login
	@test -f $(BUNDLES)/js/app.js || { \
		echo "app bundle not found; run 'make app' first"; \
		exit 1; \
	}
	cd $(WEB) && bunx vite build --outDir ../build/private --emptyOutDir
	cd build/private && vercel link --yes --project flagon > /dev/null && vercel deploy --yes

dev: ## start the development server on port 5273
	cd $(WEB) && bun run dev

serve: ## serve the built site with its proxy
	cd $(WEB) && bun run proxy -- --root dist

check: ## run the same checks used by CI
	@$(MAKE) bundles
	@$(MAKE) lint
	@$(MAKE) typecheck
	@$(MAKE) test
	@$(MAKE) browser

report: ## open the latest Playwright report
	cd $(WEB) && bunx playwright show-report

merge-reports: ## merge sharded Playwright reports
	cd $(WEB) && bunx playwright merge-reports --reporter html ./blob-report

watch: ## watch the current pull request's GitHub checks
	gh pr checks --watch --interval 30

version: ## set the project version (VERSION=1.2.3)
	@test -n "$(VERSION)" || { \
		echo "usage: make version VERSION=1.2.3"; \
		exit 1; \
	}
	sed -i.bak 's/^version = .*/version = $(VERSION)/' gradle.properties
	rm -f gradle.properties.bak

release: ## create and push a release tag (VERSION=1.2.3)
	@test -n "$(VERSION)" || { \
		echo "usage: make release VERSION=1.2.3"; \
		exit 1; \
	}
	@test "$$(git rev-parse --abbrev-ref HEAD)" = main || { \
		echo "release must be created from main"; \
		exit 1; \
	}
	@test -z "$$(git status --porcelain)" || { \
		echo "working tree is not clean"; \
		exit 1; \
	}
	git pull --ff-only
	@$(MAKE) version VERSION=$(VERSION)-SNAPSHOT
	git commit -am "$(VERSION)-SNAPSHOT"
	git tag "v$(VERSION)"
	git push origin main "v$(VERSION)"

publish: ## publish Gradle artifacts to the package registry
	$(GRADLE) publish

clean: ## remove generated build and test output
	$(GRADLE) clean
# the java language server still writes classes into target/
	find . -type d -name target -not -path './web/node_modules/*' -prune -exec rm -rf {} +
	rm -rf \
		$(BUNDLES) \
		$(WEB)/dist \
		$(WEB)/test-results \
		$(WEB)/playwright-report \
		$(WEB)/blob-report
