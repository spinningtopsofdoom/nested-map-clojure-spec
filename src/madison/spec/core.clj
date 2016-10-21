(ns madison.spec.core
  (:require
    [clojure.spec :as s]
    [clojure.test.check.generators :as gen]
    [clojure.pprint :as pprint]
    [clojure.pprint :as pp]))
;;Pets
(s/def :pet/type #{:cat :dog})
(s/def :pet/id (s/or :uuid uuid? :string string?))
(s/def :cat/name #{"Meowy Meowy Meowerson" "Leaney" "Tux"})
(s/def :dog/name #{"Kali" "Pearl" "Sweet Molly"})
(defmulti pets :type)
(defmethod pets :cat [_]
  (s/keys :req-un [:pet/type :pet/id :cat/name]))
(defmethod pets :dog [_]
  (s/keys :req-un [:pet/type :pet/id :dog/name]))
(s/def ::pets (s/multi-spec pets :type))
;;User
(s/def ::person-id (s/or :new-id uuid? :old-id pos-int?))
(s/def ::first-name #{"Joe" "Jane" "Mary" "Larry"})
(s/def ::last-name #{"Smith" "Doe" "Thompson" "Sue"})
(s/def :person/pet ::pets)
(s/def ::person
  (s/keys :req-un [::person-id ::first-name ::last-name]
          :opt-un [:person/pet]))

(s/def ::street #{"123 Main Street" "563 Willow Ave" "42 Deep Thought Way"})
(s/def ::city #{"Madison" "Green Bay" "Milwakuee"})
(s/def ::state #{"Wi"})
(s/def ::zipcode (s/int-in 10000 99999))
(s/def ::persons (s/coll-of ::person))

(s/def ::house
  (s/keys :req-un [::street ::city ::state ::zipcode ::persons]))

(defn working-generator
  ([spec] (working-generator spec 10))
  ([spec times]
    (let [samples (vec (gen/sample (s/gen spec) times))]
      (reduce-kv
        (fn [m _ v]
          (if (s/valid? spec v)
            m
            (-> m
                (assoc :valid false)
                (update :examples #(conj % (s/explain-data spec v))))))
        {:valid true :examples []} samples))))

(comment
  ; Create a house nested map
  (pprint/pprint (gen/generate (s/gen ::house)))
  ; Redefine person id generator so that 10 integeres are created for every uuid
  {:street  "42 Deep Thought Way",
   :city    "Milwakuee",
   :state   "Wi",
   :zipcode 10006,
   :persons
            [{:person-id  #uuid "5de50d62-3fe9-4b45-a3c1-539274b43aef",
              :first-name "Jane",
              :last-name  "Smith",
              :pet        {:type :dog, :id "0ohPQdnVMDHZdwwua6tAdxH", :name "Pearl"}}]}
   (s/with-gen ::person-id
               #(gen/frequency [[10 gen/pos-int]
                                [1 gen/uuid]]))
  (pprint/pprint (gen/sample (s/gen ::person-id) 5))
   ; Create updated house nested map
            {:street  "563 Willow Ave",
             :city    "Milwakuee",
             :state   "Wi",
             :zipcode 77956,
             :persons [{:person-id 1, :first-name "Joe", :last-name "Doe"}]}
   (pprint/pprint (gen/generate (s/gen ::house)))
   ; Show off explain with bad data
            (def bad-house
              {:street  "563 Willow Ave",
               :city    "Madison",
               :state   "Wi",
               :zipcode 69728,
               :persons
                        [{:person-id  #uuid "1b0aa65f-9f58-46fa-ad3a-f568ee5dd882",
                          :first-name "Jane",
                          :last-name  "Doe"
                          :pet        {:type :cat :name "Leaney" :id "123"}}
                         {:person-id  3,
                          :first-name "Joe",
                          :last-name  "Smith"
                          :pet        {:type :dog :name "Kali" :id 3}}]})
   ; Explain bad data
   (pprint/pprint (s/explain-data ::house bad-house))
   ; =>
            #:clojure.spec{:problems
                           ({:path [:persons :pet :dog :id :uuid],
                             :pred uuid?,
                             :val  3,
                             :via
                                   [:madison.spec.core/house
                                    :madison.spec.core/persons
                                    :madison.spec.core/person
                                    :madison.spec.core/pets
                                    :pet/id],
                             :in   [:persons 1 :pet :id]}
                             {:path [:persons :pet :dog :id :string],
                              :pred string?,
                              :val  3,
                              :via
                                    [:madison.spec.core/house
                                     :madison.spec.core/persons
                                     :madison.spec.core/person
                                     :madison.spec.core/pets
                                     :pet/id],
                              :in   [:persons 1 :pet :id]})}
   ; Change pet id's and make pets optional
   (s/def :pet/id uuid?)
            (s/def :person/pet (s/nilable ::pets))
   ; Rerun explain-data
   (pprint/pprint (s/explain-data ::house bad-house))
   ; =>
            #:clojure.spec{:problems
                           ({:path [:persons :pet :clojure.spec/nil],
                             :pred nil?,
                             :val  {:type :dog, :name "Kali", :id 3},
                             :via
                                   [:madison.spec.core/house
                                    :madison.spec.core/persons
                                    :madison.spec.core/person
                                    :person/pet],
                             :in   [:persons 1 :pet]}
                             {:path [:persons :pet :clojure.spec/pred :dog :id],
                              :pred uuid?,
                              :val  3,
                              :via
                                    [:madison.spec.core/house
                                     :madison.spec.core/persons
                                     :madison.spec.core/person
                                     :person/pet
                                     :madison.spec.core/pets
                                     :pet/id],
                              :in   [:persons 1 :pet :id]})})