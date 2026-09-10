(ns advocacy.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [advocacy.governor :as governor]
            [advocacy.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-member! st {:member-id "member-1" :name "Alice Member" :status :active})
    st))

(deftest hard-violations-on-unregistered-member
  (let [st (fresh-store)
        request {:member-id "no-such-member" :op :draft-correspondence :stake :low}
        proposal {:op :draft-correspondence :effect :propose :confidence 0.9 :stake :low}
        verdict (governor/check request {} proposal st)]
    (is (false? (:ok? verdict)))
    (is (true? (:hard? verdict)))
    (is (false? (:escalate? verdict)))
    (is (some? (seq (:violations verdict))))))

(deftest hard-violation-on-non-propose-effect
  (let [st (fresh-store)
        request {:member-id "member-1" :op :draft-correspondence}
        proposal {:op :draft-correspondence :effect :commit :confidence 0.9}
        verdict (governor/check request {} proposal st)]
    (is (false? (:ok? verdict)))
    (is (true? (:hard? verdict)))))

(deftest hard-violation-on-binding-authority-attempt
  (let [st (fresh-store)
        request {:member-id "member-1" :op :draft-correspondence :no-binding-authority true}
        proposal {:op :draft-correspondence :effect :propose :confidence 0.9}
        verdict (governor/check request {} proposal st)]
    (is (false? (:ok? verdict)))
    (is (true? (:hard? verdict)))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        request {:member-id "member-1" :op :draft-correspondence :stake :low}
        proposal {:op :draft-correspondence :effect :propose :confidence 0.4 :stake :low}
        verdict (governor/check request {} proposal st)]
    (is (false? (:ok? verdict)))
    (is (false? (:hard? verdict)))
    (is (true? (:escalate? verdict)))))

(deftest escalates-on-sensitive-topic
  (let [st (fresh-store)
        request {:member-id "member-1" :op :draft-correspondence}
        proposal {:op :draft-correspondence :effect :propose :confidence 0.9 :stake :medium}
        context {:topic :member-dispute}
        verdict (governor/check request context proposal st)]
    (is (false? (:ok? verdict)))
    (is (false? (:hard? verdict)))
    (is (true? (:escalate? verdict)))))

(deftest escalates-on-flag-member-conflict
  (let [st (fresh-store)
        request {:member-id "member-1" :op :flag-member-conflict}
        proposal {:op :flag-member-conflict :effect :propose :confidence 0.95}
        verdict (governor/check request {} proposal st)]
    (is (false? (:ok? verdict)))
    (is (false? (:hard? verdict)))
    (is (true? (:escalate? verdict)))))

(deftest accepts-clean-low-risk-request
  (let [st (fresh-store)
        request {:member-id "member-1" :op :draft-correspondence :stake :low}
        proposal {:op :draft-correspondence :effect :propose :confidence 0.95 :stake :low}
        verdict (governor/check request {:topic :neutral} proposal st)]
    (is (true? (:ok? verdict)))
    (is (false? (:hard? verdict)))
    (is (false? (:escalate? verdict)))))
