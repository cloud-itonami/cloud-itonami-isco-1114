(ns advocacy.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [advocacy.actor :as actor]
            [advocacy.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-member! st {:member-id "member-1" :name "Alice Member" :status :active})
    st))

(deftest commits-a-clean-low-risk-request
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:member-id "member-1" :op :draft-correspondence :stake :low}
        result (actor/run-request! graph request {:topic :neutral} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "member-1"))))))

(deftest holds-on-unregistered-member-without-committing
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:member-id "no-such-member" :op :draft-correspondence :stake :low}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (nil? (get-in result [:state :record])))
    (is (empty? (store/records-of st "no-such-member")))
    (is (= :hold (:disposition (:state result))))))

(deftest interrupts-then-commits-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        ;; flagging member conflict always escalates (governor invariant)
        request {:member-id "member-1" :op :flag-member-conflict :stake :high}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "member-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (some? (get-in resumed [:state :record])))
      (is (= 1 (count (store/records-of st "member-1")))))))

(deftest interrupts-on-sensitive-topic
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:member-id "member-1" :op :draft-position-paper :stake :medium}
        interrupted (actor/run-request! graph request {:topic :member-dispute} "thread-4")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "member-1")))))
