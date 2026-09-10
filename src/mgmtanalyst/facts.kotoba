(ns mgmtanalyst.facts
  "Well-formedness of the values the ISCO-08 2421 management and organization
  analysts actor governs: the client record, the registered engagement, and
  the proposal envelope.

  Runtime: portable `.cljc` (pure predicates, no host interop). Deliberately
  no `clojure.string` dependency — `blank?` is spelled out below so this
  namespace adds no coordinate to `deps.edn`.

  Why this namespace exists — four measurements on the pre-change tree.

  1. A registered engagement with no `:max-claimed-savings-pct` did not
     refuse. It THREW:

         (register-engagement! s {:engagement-id \"e1\" :client-id \"c1\"
                                  :baseline-metrics #{\"opex\"}})
         (governor/check <approve, :claimed-savings-pct 900> ...)
         => NullPointerException

     `(> 900 nil)` is not a refusal, it is a crash, and a crash is not a
     verdict a caller can audit. The savings ceiling is the arithmetic half of
     this repo's headline claim; an engagement registered without one turns
     that half into an exception on the exact request it exists to stop. A
     registered record that cannot be compared against is a defect in the
     registration, and is reported as one rather than thrown from the middle
     of a check.

  2. The ceiling clause was guarded by `(number? claimed-savings-pct)`, so a
     non-numeric claim skipped the comparison entirely:

         <approve, :claimed-savings-pct \"900\">  => {:ok? true :violations []}
         <approve, :claimed-savings-pct absent>  => {:ok? true :violations []}

     The guard was written to avoid the crash in (1) and had the effect of
     making the ceiling optional: anything that is not a number is under every
     ceiling. A claim the governor cannot compare is not a conforming claim,
     it is an unreadable one.

  3. Client provenance was written as `(nil? client-record)`. That asks
     whether the store returned something, not whether that something
     identifies a client. Registering the empty map put a record under the key
     `nil`, after which a request carrying no `:client-id` resolved to it:

         (register-client! s {})
         (governor/check {} {} <approve against an engagement whose
                                :client-id is also nil> s)
         => {:ok? true :violations []}

     `nil?` is a fact about the store's return value. Provenance is a fact
     about the record. Those are different questions, and the second one needs
     a place to live.

  4. `:confidence` is compared against the governor's floor to decide
     escalation, but nothing constrained it:

         <approve, :confidence 99.0>  => {:ok? true}   ; not escalated

     A confidence above the floor buys the advisor out of human review, so an
     unbounded confidence is an advisor that can decline to be reviewed. The
     floor is only a floor if the quantity it bounds is on the scale the floor
     is stated in.

  Each predicate returns a *reason keyword* or nil rather than a boolean, so
  the governor can say which fact failed instead of reporting a bare
  malformed-input."
  )

(defn- blank? [s] (or (nil? s) (and (string? s) (= 0 (count (.trim ^String (str s)))))))

(defn- name-string?
  "A usable identity string: present, a string, and not whitespace."
  [x]
  (and (string? x) (not (blank? x))))

(defn client-defect
  "Why `record` cannot serve as client provenance, or nil if it can.

  Checked against `requested-id` so that the record the store returned is the
  record the request asked for — resolving a request with no `:client-id` to a
  record registered under the key nil is exactly the hole measured above."
  [record requested-id]
  (cond
    (nil? record)                  :client/unregistered
    (not (map? record))            :client/not-a-record
    (not (name-string? requested-id))    :client/no-requested-id
    (not (name-string? (:client-id record))) :client/record-has-no-id
    (not= (:client-id record) requested-id) :client/id-mismatch
    :else nil))

(defn engagement-defect
  "Why `e` cannot be governed as a registered engagement, or nil if it can.

  `:baseline-metrics` must be a non-empty set of identity strings: the
  membership invariant is 'every cited metric is registered', which an empty
  or absent set satisfies vacuously for a recommendation that cites nothing.
  A registered engagement with nothing to cite is not a stricter engagement,
  it is an unusable one.

  `:max-claimed-savings-pct` must be a number in [0, 100]. This is the value
  whose absence threw in measurement (1)."
  [e]
  (let [m (:baseline-metrics e)
        c (:max-claimed-savings-pct e)]
    (cond
      (nil? e)                          :engagement/unregistered
      (not (map? e))                    :engagement/not-a-record
      (not (name-string? (:engagement-id e))) :engagement/no-id
      (not (name-string? (:client-id e)))     :engagement/no-client-id
      (not (set? m))                    :engagement/baseline-metrics-not-a-set
      (empty? m)                        :engagement/baseline-metrics-empty
      (not (every? name-string? m))           :engagement/baseline-metric-not-a-name
      (not (number? c))                 :engagement/no-savings-ceiling
      (not (<= 0 c 100))                :engagement/savings-ceiling-out-of-range
      :else nil)))

(defn confidence-defect
  "Why `c` cannot be compared against the escalation floor, or nil if it can.
  A confidence that is not a number on [0,1] is unusable, not merely high."
  [c]
  (cond
    (not (number? c))   :confidence/not-a-number
    #?(:clj (Double/isNaN (double c)) :cljs (js/isNaN c)) :confidence/not-a-number
    (not (<= 0 c 1))    :confidence/out-of-range
    :else nil))

(defn claimed-savings-defect
  "Why `pct` cannot be compared against the registered ceiling, or nil if it
  can. Applies only where the operation binds to an engagement; the governor
  decides that from `mgmtanalyst.operation/engagement-op?`, not from here."
  [pct]
  (cond
    (not (number? pct)) :savings/not-a-number
    #?(:clj (Double/isNaN (double pct)) :cljs (js/isNaN pct)) :savings/not-a-number
    (neg? pct)          :savings/negative
    :else nil))

(defn cited-metrics-defect
  "Why `ms` cannot be checked for membership, or nil if it can.

  An empty citation set is a defect for an engagement-bound operation: a
  recommendation that cites nothing passes 'every cited metric is registered'
  vacuously, which was measured admitting an evidence-free recommendation on
  the pre-change tree."
  [ms]
  (cond
    (not (set? ms))        :metrics/not-a-set
    (empty? ms)            :metrics/none-cited
    (not (every? name-string? ms)) :metrics/not-a-name
    :else nil))
