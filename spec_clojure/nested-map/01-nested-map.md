!SLIDE

# Our Sample Bad Data

    @@@ clojure
    {:street  "563 Willow Ave",
     :city    "Madison",
     :state   "WI",
     :zipcode 69728,
     :persons [{:person-id  #uuid "1b0aa65f-9f58-46fa-ad3a-f568ee5dd882",
                :first-name "Jane",
                :last-name  "Doe"
                :pet {:type :cat :name "Leaney" :id "123"}}
               {:person-id 3,
                :first-name "Joe",
                :last-name "Smith"
                :pet {:type :dog :name "Kali" :id 3}}]}

!SLIDE
# Better start digging through server logs

	@@@ clojure
    (defn clean-id [id]
      (if (uuid? id)
        id
       (-> id cs/trim cs/lower-case)))

    ; => ClassCastException java.lang.Long cannot be
    ;    cast to java.lang.CharSequence
    ;    clojure.string/trim (string.clj:234)

!SLIDE

# Have fun digging through your nested map

	@@@ clojure
    (defn valid-id [id]
      (when-not (or (uuid? id) (string? id))
      (str "The id (" id ") is not a string or UUID")))

    ; => "The id (3) is not a string or UUID"

!SLIDE

# Lets make our specs starting with House

    @@@ clojure
    (s/def ::house
	(s/keys :req-un [::street
				     ::city
				     ::state
				     ::zipcode
				     ::persons]))

!SLIDE

# House Keys

    @@@ clojure
    (s/def ::street #{"123 Main Street"
                      "563 Willow Ave"
                      "42 Deep Thought Way"})
    (s/def ::city #{"Madison" "Green Bay" "Milwakuee"})
    (s/def ::state #{"WI"})
    (s/def ::zipcode (s/int-in 10000 99999))
    (s/def ::persons (s/coll-of ::person))

!SLIDE

# Person

    @@@@ clojure
    (s/def ::person-id (s/or :new-id uuid? :old-id pos-int?))
    (s/def ::first-name #{"Joe" "Jane" "Mary" "Larry"})
    (s/def ::last-name #{"Smith" "Doe" "Thompson" "Sue"})
	(s/def :person/pet ::pets)
    (s/def ::person
      (s/keys :req-un [::person-id
                       ::first-name
                       ::last-name]
              :opt-un [:person/pet]))

!SLIDE

# Pet

    @@@ clojure
    (s/def :pet/type #{:cat :dog})
	(s/def :pet/id (s/or :uuid uuid? :string string?))
    (s/def :cat/name #{"Meowy Meowerson" "Leaney" "Tux"})
    (s/def :dog/name #{"Kali" "Pearl" "Sweet Molly"})
    (defmulti pets :type)
    (defmethod pets :cat [_]
      (s/keys :req-un [:pet/type :pet/id :cat/name]))
    (defmethod pets :dog [_]
      (s/keys :req-un [:pet/type :pet/id :dog/name]))
    (s/def ::pets (s/multi-spec pets :type))

!SLIDE

# Nice things to note
- All registered specs have a globally unique name
- You can refer to a spec from another spec before you've defined it

!SLIDE

# Let's generate the house

    @@@ clojure
    (gen/generate (s/gen ::house))
    ; =>
    {:street  "42 Deep Thought Way",
     :city    "Milwakuee",
     :state   "WI",
     :zipcode 10006,
     :persons [{:person-id  #uuid "5de50d62-3fe9-4b45-a3c1-539274b43aef",
                :first-name "Jane",
                :last-name  "Smith",
                :pet {:type :dog,
                      :id "0ohPQdnVMDHZdwwua6tAdxH",
                      :name "Pearl"}}]}

!SLIDE

# Changing the generator for `::person-id`

    @@@ clojure
    (s/def ::person-id
        (s/spec ::person-id
                :gen #(gen/frequency [[10 gen/pos-int]
                                      [1 gen/uuid]])))
    (gen/sample (s/gen ::person-id) 5)
    ; =>
    [2 2 3 2
     #uuid "0f777b60-2f5b-458d-a22d-0998a94a7fa0"]

!SLIDE

# What happens when we get bad data

    @@@ clojure
    {:street  "563 Willow Ave",
	 :city    "Madison",
	 :state   "WI",
	 :zipcode 69728,
	 :persons [{:person-id  #uuid "1b0aa65f-9f58-46fa-ad3a-f568ee5dd882",
				:first-name "Jane",
				:last-name  "Doe"
				:pet {:type :cat :name "Leaney" :id "cat123"}}
			   {:person-id 3,
				:first-name "Joe",
				:last-name "Smith"
				:pet {:type :dog :name "Kali" :id 3}}]}

!SLIDE

# Results of `(s/explain-data ::house bad-house)`

    @@@ clojure
    #:clojure.spec{:problems
                 ({:path [:persons :pet :dog :id :uuid],
                   :pred uuid?,
                   :val 3,
                   :via [:madison.spec.core/house
                         :madison.spec.core/persons
                         :madison.spec.core/person
                         :madison.spec.core/pets
                         :pet/id],
                   :in [:persons 1 :pet :id]})}

!SLIDE

<br />
<br />
<br />
# What do `:in`, `:via` and `:path` tell us?
## ¯&#92;\_(ツ)\_/¯ they're all synonyms of each other?

!SLIDE

# `:in` tells us the path through the map to get the `:val`

	@@@ clojure
	(get-in[:persons 1 :pet :id] bad-house) ; => 3

    {:street  "563 Willow Ave",
	 :city    "Madison",
	 :state   "WI",
	 :zipcode 69728,
	 :persons [{:person-id  #uuid "1b0aa65f-9f58-46fa-ad3a-f568ee5dd882",
				:first-name "Jane",
				:last-name  "Doe"
				:pet {:type :cat :name "Leaney" :id "cat123"}}
			   {:person-id 3,
				:first-name "Joe",
				:last-name "Smith"
				:pet {:type :dog :name "Kali" :id 3}}]}

!SLIDE

# `:via` gives us the spec decision tree that got us to this failing spec

	@@@ clojure
	[:madison.spec.core/house
	 :madison.spec.core/persons
	 :madison.spec.core/person
	 :madison.spec.core/pets
	 :pet/id]

!SLIDE

<br/ >
# So what about `:path`?
<br/ >
## `[:persons :pet :dog :id :uuid]` doesn't seem to fit either the data or spec names.
<br/ >
## It's really pretty subtle

!SLIDE

# It tells us how we choose which specs to apply

	@@@ clojure
    :persons ; => :madison.spec.core/persons
    :pet     ; => :madison.spec.core/pets
    :dog     ; => :dog method spec from pets mutlispec
    :id      ; => :pets/id
    :uuid    ; => uuid?

!SLIDE

<br/ >
<br/ >
<br/ >
<br/ >
# Many validation libraries will just tell you `3` failed `uuid?`

!SLIDE

# `:in`, `:via` and `:path` give us

- How to get to the failing value
- The spec decision tree that led to the failing spec
- Why these exact specs were chosen
- The correct locations to look
  - Looking at wrong values
  - Guessing the decision tree leading to failure
  - Guessing why that validation path was used
  - Thinking the data is bad when the validation should be changed
