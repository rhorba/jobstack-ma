# CORRECTIONS — JobStack.ma



## 2026-07-20 — Local verification process bug (self-caught after CI red)
Every local `mvn test`/`mvn verify` background run this session was piped through `| tail -N` for readability. That pattern makes the reported shell exit code reflect `tail` (always 0), not `mvn` — so my "final backend verify: exit 0, all green" claims before this session's SHIP were not actually verified by exit code, only by eyeballing tail output that happened not to show the failure lines prominently. CI's unpiped `mvn verify` caught 2 real test failures (stale `.get(0)` JSON assertions after the applicant-list endpoint became paginated) that my local run had silently swallowed.
Why it matters: exit-code-reliant verification is only trustworthy when nothing sits between the command and the exit code check — piping through tail/grep/head silently breaks this.
How to apply: for any future mvn/npm run whose PASS/FAIL result gates a decision (especially before a push), either (a) don't pipe to tail — write to a background output file and grep that file for BUILD FAILURE/ERROR/Tests run afterward, or (b) capture the real exit code explicitly (e.g. run un-piped and check $? before any pipe). Confirmed this concretely mid-session: `cmd | tail` reported exit 0 even when `cmd` itself exited 1 (reproduced deliberately while testing the frontend coverage threshold gate).
