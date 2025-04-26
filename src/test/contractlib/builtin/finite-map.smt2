(declare-sort Set 1)
(declare-sort Map 2)

(declare-fun map.empty
  (par (A B) () (Map A B)))

(declare-fun map.card
  (par (A B) ((Map A B)) Int))

(declare-fun set.member
  (par (A B) (A (Map A B)) Bool))

(declare-fun map.keys
  (par (A B) ((Map A B)) (Set A)))

(declare-fun map.values
  (par (A B) ((Map A B)) (Set B)))

(declare-fun map.subset
  (par (A B) ((Map A B) (Map A B)) Bool))

(declare-fun map.singleton
  (par (A B) (A B) (Map A B)))

(declare-fun map.get
  (par (A B) ((Map A B) A) B))

(declare-fun map.update
  (par (A B) ((Map A B) (Map A B)) (Map A B)))

(declare-fun map.minus
  (par (A B) ((Map A B) (Set A)) (Map A B)))
