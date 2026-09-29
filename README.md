# cloud-itonami-isco-1114

Open Occupation Blueprint for **ISCO-08 1114**: Senior Officials of Special-interest Organizations.

This repository designs a staff-support actor for a senior official's office at a special-interest organization (trade union, industry association, advocacy group, professional society, etc.): a member-engagement advisor supports an official's administrative workflow (member correspondence, meeting scheduling, position-paper drafting, member-conflict escalation) under a governor-gated actor, **while explicitly excluding any binding organizational authority** (governance decisions, member discipline, negotiation on behalf of the organization).

## Scope & Constraints

**This actor supports an official's OFFICE, not an official as a decision-making agent.** It is a staff-support tool, never a replacement for human judgment or the official's organizational authority. The actor explicitly cannot:

- issue binding organizational decisions or governance directives
- represent itself as speaking for the organization or negotiating on its behalf (proposals only)
- claim or exercise binding power over member discipline, membership status, or organizational disputes
- disclose member or internal organization data without verification and human review
- propose external negotiations or policy stances without explicit human review

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs the physical domain work**. Here a member-engagement advisor supports an official's office administrative workflow under an actor that proposes actions and an independent **Advocacy Governor** that gates them. The governor never dispatches actions without human gating; escalation-category proposals (member disputes, external negotiations, controversial policy stances) require explicit human sign-off.

## Core Contract

```text
member inquiry + organizational records + office context
        |
        v
Advocacy Advisor -> Advocacy Governor -> draft correspondence/position/meeting, or escalate
        |
        v
office actions (gated) + operating records + audit ledger
```

No automated advice can dispatch an office action the governor refuses, suppress an operating record, or disclose member/organizational data without governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation) (ISCO-08 `1114`). Required capabilities:

- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and [`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation (`:maturity :implemented`)

Full itonami Actor pattern (per ADR-2607011000 / AGENTS.md's Actors section): a real [`kotoba-lang/langgraph`](https://github.com/kotoba-lang/langgraph) `StateGraph`, with the Advisor and Governor as distinct graph nodes and human-in-the-loop interrupt/resume via checkpointing.

```text
:intake -> :advise -> :govern -> :decide -+-> :commit            (:ok? true)
                                           +-> :request-approval   (:escalate? true, interrupt-before)
                                           +-> :hold               (:hard? true)
```

- `src/advocacy/store.kotoba` — `Store` protocol + `MemStore`: registered members, committed records, an append-only audit ledger.
- `src/advocacy/advisor.kotoba` — `Advisor` protocol; `mock-advisor` (deterministic, default) proposes an office operation from a request; `llm-advisor` wraps a `langchain.model/ChatModel` — either way the advisor only ever produces a `:propose`-effect proposal, never a committed record, and LLM parse failures always yield `confidence 0.0` (forces escalation, never fabricated confidence).
- `src/advocacy/governor.kotoba` — `AdvocacyGovernor/check`: a pure function, wired as its own `:govern` node. Hard invariants (unregistered member, a proposal whose `:effect` isn't `:propose`, attempts at binding organizational power) always route to `:hold`. Escalation invariants (`:flag-member-conflict`, member disputes, external negotiations, controversial topics, or low advisor confidence) always route to `:request-approval` — an `interrupt-before` node that the graph checkpoints and only resumes on explicit human approval (`actor/approve!`).
- `src/advocacy/actor.kotoba` — `build-graph`, `run-request!`, `approve!`: the `langgraph.graph/state-graph` wiring itself.

Proposal ops (all `:effect :propose` only, closed allowlist):
- `:draft-correspondence` — prepare a reply to member/stakeholder correspondence.
- `:schedule-meeting` — prepare a meeting schedule/invitation for member engagement.
- `:draft-position-paper` — prepare a position-paper draft for the official's own review and decision.
- `:flag-member-conflict` — surface a member/stakeholder conflict for the official's attention (always escalates).

```bash
kbb -M:test
```

This is what backs this repo's `:maturity :implemented` entry in [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation).

## License

AGPL-3.0-or-later.
