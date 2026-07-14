# Operator Guide

## Setup

```clojure
(require '[advocacy.actor :as actor]
         '[advocacy.store :as store])

(def st (store/mem-store))
(store/register-member! st {:member-id "alice" :name "Alice" :status :active})

(def graph (actor/build-graph {:store st}))
```

## Running a request

```clojure
;; Draft a reply to member correspondence
(let [request {:member-id "alice" :op :draft-correspondence :stake :low}
      result (actor/run-request! graph request {:topic :neutral} "thread-1")]
  result)
```

## Handling interrupts

When the Governor escalates (sensitive topic, member dispute, low confidence), the run returns `:interrupted`:

```clojure
(let [request {:member-id "alice" :op :flag-member-conflict}
      interrupted (actor/run-request! graph request {} "thread-2")]
  ;; Wait for human review
  (let [resumed (actor/approve! graph "thread-2")]
    resumed))
```

## Store operations

- `(store/register-member! st member)` — register a member
- `(store/member st member-id)` — look up a member
- `(store/records-of st member-id)` — view committed records for a member
- `(store/ledger st)` — inspect the full audit ledger
- `(store/commit-record! st record)` — (only used internally by actor; never called directly)

## Audit trail

The store maintains an append-only ledger of all proposals, verdicts, and outcomes:

```clojure
(store/ledger st)
;; => [{:node :advise :request ... :proposal ...}
;;     {:node :govern :verdict {...}}
;;     {:disposition :commit :record {...}}]
```

## Proposal operations

All proposals have `:effect :propose` only (no direct dispatch):

- `:draft-correspondence` — prepare a reply to member correspondence
- `:schedule-meeting` — prepare a meeting schedule/invitation
- `:draft-position-paper` — prepare a position-paper draft
- `:flag-member-conflict` — surface a member/stakeholder conflict

## Governor rules

Hard violations (always `:hold`, no appeal):
- Unregistered member
- Non-`:propose` effect (no direct actions)
- Attempt to exercise binding organizational authority

Escalation triggers (always `:request-approval`, human sign-off required):
- `:flag-member-conflict` (by definition)
- Sensitive topics (`:member-dispute`, `:external-negotiation`, `:policy-stance`, `:member-discipline`)
- Low advisor confidence (< 0.6)
