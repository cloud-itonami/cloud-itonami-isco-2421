(ns mgmtanalyst.store
  "SSoT for the ISCO-08 2421 community management & organization
  analysts actor (itonami actor pattern, ADR-2607011000 / CLAUDE.md
  Actors section). Modeled on cloud-itonami-isco-4311's
  bookkeeping.store.

  Domain:

    client     — a registered organization (:client-id, :name)
    engagement — a registered consulting engagement {:engagement-id
                 :client-id :name :baseline-metrics #{metric-str}
                 :max-claimed-savings-pct number}.
                 `:baseline-metrics` is the registered set of metrics
                 a recommendation may cite as evidence (no invented
                 metric); `:max-claimed-savings-pct` is the registered
                 ceiling a proposed savings claim must not exceed
                 (excessive claims are marketing, not analysis).
    record     — a committed operating record (approved
                 recommendation) — written ONLY via commit-record!.
    ledger     — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (engagement [s engagement-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-engagement! [s e])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (engagement [_ engagement-id] (get-in @a [:engagements engagement-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-engagement! [s e]
    (swap! a assoc-in [:engagements (:engagement-id e)] e) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :engagements {} :records [] :ledger []}
                                   seed)))))
