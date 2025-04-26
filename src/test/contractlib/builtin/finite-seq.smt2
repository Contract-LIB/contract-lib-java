(declare-sort Seq 1)

; accessors
(declare-fun seq.len
  (par (A) ((Seq A)) Int))

(declare-fun seq.nth
  (par (A) ((Seq A) Int) A))

; non-free constructor resp. sequence display
(declare-fun seq
  (par (A) (Int (Array Int A)) (Seq A)))

; axiomatization of accessors
(assert
  (par (A)
    (forall ((n Int) (a (Array Int A)))
      (=> (<= 0 n)
          (= (seq.len (seq n a)) n)))))

(assert
  (par (A)
    (forall ((i Int) (n Int) (a (Array Int A)))
      (=> (and (<= 0 i) (< i n))
          (= (seq.nth i (seq n a)) (select a i))))))

; extensionality
(assert
  (par (A)
    (forall ((xs (Seq A)) (ys (Seq A)))
      (=> (and (= (seq.len xs) (seq.len ys))
               (forall ((i Int))
                 (=> (and (<= 0 i) (<= i (seq.len xs)))
                     (= (seq.nth i xs) (seq.nth i ys)))))
          (= xs ys)))))

; type invariant
(assert
  (par (A)
    (forall ((xs (Seq A)))
      (<= 0 (seq.len xs)))))

; definition of derived functions

(declare-const undefined
  (par (A) A))

(define-fun seq.empty
  (par (A) () (Seq A)
    (seq 0 (const undefined))))

(define-fun seq.contains
  (par (A) ((x A) (xs (Seq A))) Bool
    (exists ((i Int))
      (and (<= 0 i) (<= i n) (= (seq.nth i xs)) x))))

(define-fun seq.unit
  (par (A) ((x A)) (Seq A)
    (seq 1 (const x))))

(define-fun seq.++
  (par (A) ((xs (Seq A)) (ys (Seq A))) (Seq A)
    (seq (+ (length xs) (length ys))
        (lamnda ((i Int))
          (ite (<= i (length xs))
                (seq.nth i xs)
                (seq.nth (- i (seq.length xs))
                         ys))))))

(declare-fun seq.update
  (par (A) ((Seq A) Int (Seq A)) (Seq A)))

(declare-fun seq.extract
  (par (A) ((Seq A) Int Int) (Seq A)))