(ns advocacy.store
  "SSoT for the ISCO-08 1114 senior official administrative support actor. Store is a
  protocol injected into the `advocacy.actor` StateGraph — `MemStore`
  is the default, deterministic, zero-dep backend; a Datomic/kotoba-server-
  backed implementation can be swapped in without touching the actor or
  governor (itonami actor pattern, per ADR-2607011000 / CLAUDE.md Actors
  section).

  Domain:

    member      — a registered member or stakeholder
                  (:member-id, :name, :status)
    record      — a committed office record (correspondence drafted, meeting
                  scheduled, position-paper drafted, member conflict flagged)
                  — written ONLY via commit-record!, never mutated in place
    ledger      — an append-only audit trail of every proposal/verdict/
                  disposition, regardless of outcome (commit or hold)")

(defprotocol Store
  (member [s member-id])
  (records-of [s member-id])
  (ledger [s])
  (register-member! [s member])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (member [_ member-id] (get-in @a [:members member-id]))
  (records-of [_ member-id] (filter #(= member-id (:member-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-member! [s member]
    (swap! a assoc-in [:members (:member-id member)] member) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:members {} :records [] :ledger []} seed)))))
