(ns mgmtanalyst.advisor
  "ManagementOrganizationAnalystsAdvisor — proposes a recommendation
  operation (approve a recommendation, publish a recommendation) for
  a registered organization. Swappable mock/llm; the advisor ONLY
  proposes — `mgmtanalyst.governor` checks metric-citation membership
  and the savings-claim ceiling independently. Modeled on
  cloud-itonami-isco-4311's advisor.

  A proposal: {:op :approve-recommendation|:publish-recommendation
               :effect :propose :engagement-id str
               :cited-metrics #{str} :claimed-savings-pct number
               :stake kw :confidence n :rationale str}")

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake engagement-id cited-metrics claimed-savings-pct] :as request}]
  {:op op
   :effect :propose
   :engagement-id engagement-id
   :cited-metrics cited-metrics
   :claimed-savings-pct claimed-savings-pct
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a management and organization analysis advisor. Given a
   request, propose an :op, the :engagement-id, :cited-metrics and
   :claimed-savings-pct, an honest :confidence and a :stake. Never
   cite a fabricated metric or an over-ceiling savings claim as
   conforming — the governor checks both against the registered
   engagement record.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
