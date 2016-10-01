(ns scoper.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [scoper.core-test]))

(doo-tests 'scoper.core-test)
