(declare-abstractions
    ((Counter 0))
    (((Counter ((value Int))))))

(declare-contract Counter.init
    ((this (out Counter)))
    ((true (= (value this) 0))))

(declare-contract Counter.value
    ((this (in Counter))
     (result (out Int)))
    ((true (= result (value this)))))

(declare-contract Counter.increment
    ((this (inout Counter)))
    ((true (= (value this) (+ 1 (old (value this)))))))

(declare-contract Counter.decrement
    ((this (inout Counter)))
    (((< 0 (value this))
      (= (value this) (+ 1 (old (value this)))))))
