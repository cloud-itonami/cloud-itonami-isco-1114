(ns advocacy.governor
  "AdvocacyGovernor — the independent safety/traceability layer for the
  ISCO-08 1114 senior official administrative support actor. Wired as its own `:govern`
  node in `advocacy.actor`'s StateGraph, downstream of `:advise` — the
  Advisor has no notion of member provenance, topic sensitivity, or
  organizational-authority risk, so this MUST be a separate system able to reject a
  proposal (itonami actor pattern, per ADR-2607011000 / CLAUDE.md Actors
  section).

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. member provenance       — if member-id is provided, it must
       be registered.
    2. no-actuation            — proposal :effect must be :propose.
    3. no-binding-authority    — no :op that claims or exercises binding
       organizational power (member discipline, negotiation on the org's behalf).

  ESCALATION invariants (:escalate? true, ALWAYS human sign-off):
    4. :flag-member-conflict always escalates (sensitive by definition).
    5. topic sensitivity       — topics tagged as legally/politically/organizationally
       sensitive must escalate for official review.
    6. low confidence (< `confidence-floor`)."
  (:require [advocacy.store :as store]))

(def confidence-floor 0.6)
(def ^:private sensitive-topics #{:member-dispute :external-negotiation :policy-stance :member-discipline})
(def ^:private escalating-ops #{:flag-member-conflict})

(defn- hard-violations [{:keys [proposal request]} member-record]
  (cond-> []
    (and (some? (:member-id request))
         (nil? member-record))
    (conj {:rule :no-member :detail "unregistered member"})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect must be :propose only (no direct dispatch)"})

    (get request :no-binding-authority)
    (conj {:rule :no-binding-authority :detail "actor has no binding organizational power (member discipline, negotiation on behalf of organization)"})))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a `store`
  implementing `advocacy.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request context proposal store]
  (let [member-record (when (some? (:member-id request))
                        (store/member store (:member-id request)))
        hard (hard-violations {:proposal proposal :request request}
                              member-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        flagging-conflict? (contains? escalating-ops (:op proposal))
        topic-sensitive? (contains? sensitive-topics (:topic context))
        risky? (or flagging-conflict? topic-sensitive?)]
    {:ok? (and (not hard?) (not low?) (not risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky?))}))
