.DEFAULT_GOAL := help

MVN := ./mvnw -B -T 1C
WEB := web
BUNDLES := $(WEB)/public/flagon
ARGS ?=
N ?= 3
EXT ?=

# drop any jar in vendor/ and it is built into the `app` bundle, which /play/ runs, its libraries
# beside it if it needs any. the jar names its own main class, so nothing here needs to be told
# anything about it
VENDOR_JARS := $(wildcard vendor/*.jar)
APP := $(if $(VENDOR_JARS),-Papp,)

# name the packages a library scans, one glob for each line
PRESERVE_FILE := vendor/preserve.txt
PRESERVE := $(if $(wildcard $(PRESERVE_FILE)),-Dflagon.preserve='$(shell grep -v '^\#' $(PRESERVE_FILE) | paste -sd, -)',)

.PHONY: help setup deps browsers browsers-deps browser-chromium browser-served browser-firefox bundles bundles-js app build bundles-release \
        bundles-release-js build-release test browser flake typecheck lint site dev serve \
        check report merge-reports watch version release publish clean verify-bundles size \
        app-cost app-size

help: ## list what this repo can do
	@grep -hE '^[a-z-]+:.*## ' $(MAKEFILE_LIST) \
		| awk 'BEGIN { FS = ":.*## " }; { printf "  %-20s %s\n", $$1, $$2 }'

setup: deps browsers ## install everything a fresh checkout needs
	@$(MVN) -v > /dev/null

deps: ## install the shell's packages
	cd $(WEB) && bun install --frozen-lockfile

browsers: browsers-deps ## install the engines the tests drive
	cd $(WEB) && bunx playwright install chromium firefox

# the engine binaries cache; these are apt's, are system-wide, and do not. ci installs them only
# for the job that needs them, headless chromium having everything it wants on a runner already.
browsers-deps: ## install the system libraries the engines link against
	cd $(WEB) && bunx playwright install-deps chromium firefox

browser-chromium: ## install just the engine the sharded tests drive
	cd $(WEB) && bunx playwright install chromium

browser-served: browser-chromium ## the served run drives chromium too

browser-firefox: ## install just firefox, and the libraries it wants
	cd $(WEB) && bunx playwright install-deps firefox && bunx playwright install firefox

bundles: ## compile java to webassembly gc
	$(MVN) $(APP) $(PRESERVE) package
	@$(MAKE) verify-bundles

app: ## compile whatever jar is in vendor/ into a bundle, and say where to open it
	@test -n "$(VENDOR_JARS)" || { echo "put a jar -- any jar -- in vendor/ first"; exit 1; }
	@$(MAKE) app-cost
	@started=$$(date +%s); $(MAKE) build && echo "==> built in $$(( $$(date +%s) - started ))s"
	@$(MAKE) app-size
	@echo "run 'make dev', then open http://localhost:5273/ and pick the app bundle"
	@echo "for the console, the program picker and the argument field, or"
	@echo "http://localhost:5273/play/ to run it fullscreen"

# make 3.81 mangles a long recipe split over continuations, so these stay on one line each
app-cost: ## say how much work the jars in vendor/ are, before the build starts
	@classes=$$(for jar in $(VENDOR_JARS); do unzip -l "$$jar" | grep -c "[.]class$$"; done | awk '{ total += $$1 } END { print total + 0 }'); echo "==> $(words $(VENDOR_JARS)) jar(s), $$classes classes, compiled for each backend"

app-size: ## weigh the app bundle that both backends emitted
	@printf "%-16s %10s\n" bundle bytes; for file in $(BUNDLES)/app.wasm $(BUNDLES)/app.js; do test -f $$file && printf "%-16s %10s\n" $$(basename $$file) $$(wc -c < $$file | tr -d " "); done; true

bundles-js: ## compile java to javascript, the fallback backend
	$(MVN) -Pjs $(APP) $(PRESERVE) -DskipTests package
	@$(MAKE) verify-bundles

# a bundle compiled from class files that never compiled is small and throws at boot, and the
# build that emitted it reports success. see issue #106
verify-bundles: ## check the emitted bundles are whole
	@for bundle in $(BUNDLES)/*.js $(BUNDLES)/*.wasm; do \
		case $$bundle in *wasm-gc-module-runtime.js) continue;; esac; \
		size=$$(wc -c < $$bundle | tr -d " "); \
		test $$size -ge 100000 || { echo "$$bundle is only $$size bytes; run 'make clean'"; exit 1; }; \
		grep -q "Unresolved compilation problem" $$bundle \
			&& { echo "$$bundle carries uncompiled classes; run 'make clean'"; exit 1; }; \
	done; \
	for catalog in $(BUNDLES)/*.catalog.json; do \
		grep -q '"id"' $$catalog || { echo "$$catalog lists no programs"; exit 1; }; \
	done; echo "bundles look whole"

build: ## compile both backends
	@echo "==> webassembly, backend 1 of 2"
	@$(MAKE) bundles
	@echo "==> javascript, backend 2 of 2"
	@$(MAKE) bundles-js

bundles-release: ## compile to webassembly gc as a hosted build ships it
	$(MVN) -Prelease $(PRESERVE) package
	@$(MAKE) verify-bundles
	@$(MAKE) size EXT=wasm

bundles-release-js: ## compile to javascript as a hosted build ships it
	$(MVN) -Pjs -Prelease $(PRESERVE) -DskipTests package
	@$(MAKE) verify-bundles
	@$(MAKE) size EXT=js

build-release: ## compile both backends as a hosted build ships them
	@$(MAKE) bundles-release
	@$(MAKE) bundles-release-js

# only meaningful against a release build; a debug bundle carries names a release one drops
size: ## weigh the release bundles against toolchain/size-budget.txt (EXT=wasm for one backend)
	@over=0; \
	printf "%-16s %10s %10s %8s\n" bundle bytes budget left; \
	while read -r name limit; do \
		case $$name in \#*|"") continue;; esac; \
		test -z "$(EXT)" || case $$name in *.$(EXT)) ;; *) continue;; esac; \
		file=$(BUNDLES)/$$name; \
		test -f $$file || { echo "$$name was not built"; exit 1; }; \
		size=$$(wc -c < $$file | tr -d " "); \
		left=$$(( limit - size )); \
		printf "%-16s %10s %10s %8s\n" $$name $$size $$limit $$left; \
		test $$size -le $$limit || over=1; \
	done < toolchain/size-budget.txt; \
	test $$over -eq 0 || { echo "a bundle is over budget; make it smaller or raise the number on purpose"; exit 1; }; \
	echo "bundles are within budget"

test: ## run the jvm unit tests
	$(MVN) test

browser: ## drive the real bundles in a browser (ARGS='--shard=1/4')
	@test -d $(BUNDLES) || { echo "no bundles yet; run 'make build'"; exit 1; }
	cd $(WEB) && bunx playwright test $(ARGS)

flake: ## rerun the browser tests to shake out a flake (N=3, ARGS='tests/a11y.spec.ts')
	@test -d $(BUNDLES) || { echo "no bundles yet; run 'make build'"; exit 1; }
	cd $(WEB) && bunx playwright test --repeat-each=$(N) $(ARGS)

typecheck: ## check the shell's types
	cd $(WEB) && bun run typecheck

lint: ## check both languages for style
	$(MVN) -q checkstyle:check
	cd $(WEB) && bun run lint

site: ## build the shell for hosting
	cd $(WEB) && bun run build

dev: ## serve the shell with reload, on :5273
	cd $(WEB) && bun run dev

serve: ## serve a built site and its socket proxy together
	cd $(WEB) && bun run proxy -- --root dist

check: ## everything ci runs
	@$(MAKE) build
	@$(MAKE) lint
	@$(MAKE) typecheck
	@$(MAKE) browser

report: ## open the last browser run's report
	cd $(WEB) && bunx playwright show-report

merge-reports: ## fold the sharded reports of a ci run into one
	cd $(WEB) && bunx playwright merge-reports --reporter html ./blob-report

watch: ## follow this branch's pr checks until they settle
	gh pr checks --watch --interval 30

version: ## set the version in the poms (VERSION=1.2.3)
	@test -n "$(VERSION)" || { echo "usage: make version VERSION=1.2.3"; exit 1; }
	$(MVN) versions:set -DnewVersion=$(VERSION) -DgenerateBackupPoms=false

# the poms carry the next release as a snapshot; the tag is what the release workflow builds
release: ## cut a release from main (VERSION=1.2.3)
	@test -n "$(VERSION)" || { echo "usage: make release VERSION=1.2.3"; exit 1; }
	@test "$$(git rev-parse --abbrev-ref HEAD)" = main || { echo "releases are cut from main"; exit 1; }
	@test -z "$$(git status --porcelain)" || { echo "the working tree is dirty"; exit 1; }
	git pull --ff-only
	@$(MAKE) version VERSION=$(VERSION)-SNAPSHOT
	git commit -am $(VERSION)-SNAPSHOT
	git tag v$(VERSION)
	git push origin main v$(VERSION)

publish: ## deploy the artifacts to the package registry
	$(MVN) deploy -DskipTests

clean: ## remove every build output
	$(MVN) -q clean
	rm -rf $(BUNDLES) $(WEB)/dist $(WEB)/test-results $(WEB)/playwright-report $(WEB)/blob-report
