.DEFAULT_GOAL := help

MVN := ./mvnw -B
WEB := web
BUNDLES := $(WEB)/public/flagon
ARGS ?=
N ?= 3

.PHONY: help setup deps browsers bundles bundles-js build bundles-release \
        bundles-release-js build-release test browser flake typecheck site dev serve \
        check report merge-reports watch version release publish clean verify-bundles

help: ## list what this repo can do
	@grep -hE '^[a-z-]+:.*## ' $(MAKEFILE_LIST) \
		| awk 'BEGIN { FS = ":.*## " }; { printf "  %-20s %s\n", $$1, $$2 }'

setup: deps browsers ## install everything a fresh checkout needs
	@$(MVN) -v > /dev/null

deps: ## install the shell's packages
	cd $(WEB) && bun install --frozen-lockfile

browsers: ## install the engines the tests drive
	cd $(WEB) && bunx playwright install-deps chromium firefox && bunx playwright install chromium firefox

bundles: ## compile java to webassembly gc
	$(MVN) package
	@$(MAKE) verify-bundles

bundles-js: ## compile java to javascript, the fallback backend
	$(MVN) -Pjs -DskipTests package
	@$(MAKE) verify-bundles

# a bundle compiled from class files that never compiled is small and throws at boot, and the
# build that emitted it reports success. see issue #106
verify-bundles: ## check the emitted bundles are whole
	@for bundle in $(BUNDLES)/*.js $(BUNDLES)/*.wasm; do \
		case $$bundle in *wasm-gc-module-runtime.js) continue;; esac; \
		size=$$(wc -c < $$bundle | tr -d " "); \
		test $$size -ge 250000 || { echo "$$bundle is only $$size bytes; run 'make clean'"; exit 1; }; \
		grep -q "Unresolved compilation problem" $$bundle \
			&& { echo "$$bundle carries uncompiled classes; run 'make clean'"; exit 1; }; \
	done; echo "bundles look whole"

build: ## compile both backends
	@$(MAKE) bundles
	@$(MAKE) bundles-js

bundles-release: ## compile to webassembly gc as a hosted build ships it
	$(MVN) -Prelease package
	@$(MAKE) verify-bundles

bundles-release-js: ## compile to javascript as a hosted build ships it
	$(MVN) -Pjs -Prelease -DskipTests package
	@$(MAKE) verify-bundles

build-release: ## compile both backends as a hosted build ships them
	@$(MAKE) bundles-release
	@$(MAKE) bundles-release-js

test: ## run the jvm unit tests
	$(MVN) test

browser: ## drive the real bundles in a browser (ARGS='--shard=1/3')
	@test -d $(BUNDLES) || { echo "no bundles yet; run 'make build'"; exit 1; }
	cd $(WEB) && bunx playwright test $(ARGS)

flake: ## rerun the browser tests to shake out a flake (N=3, ARGS='tests/a11y.spec.ts')
	@test -d $(BUNDLES) || { echo "no bundles yet; run 'make build'"; exit 1; }
	cd $(WEB) && bunx playwright test --repeat-each=$(N) $(ARGS)

typecheck: ## check the shell's types
	cd $(WEB) && bun run typecheck

site: ## build the shell for hosting
	cd $(WEB) && bun run build

dev: ## serve the shell with reload, on :5273
	cd $(WEB) && bun run dev

serve: ## serve a built site and its socket proxy together
	cd $(WEB) && bun run proxy -- --root dist

check: ## everything ci runs
	@$(MAKE) build
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
