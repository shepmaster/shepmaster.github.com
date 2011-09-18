(require '[clojure.set :as set])

;; I feel that there are too many explicit conversions with `set`, but
;; I'm not sure how to remove them.

;; Reusable test cells
(def center {:x 0 :y 0})
(def top-left {:x -1 :y 1})
(def top-right {:x 1 :y 1})
(def bottom-left {:x -1 :y -1})
(def bottom-right {:x 1 :y -1})

(defn neighbors
  "Finds all cells that are neighbors of the given cell"
  [cell cells]
  (letfn [(neighbor? [poss]
            (and (>= (poss :x) (dec (cell :x)))
                 (<= (poss :x) (inc (cell :x)))
                 (>= (poss :y) (dec (cell :y)))
                 (<= (poss :y) (inc (cell :y)))
                 (not (= poss cell))))]
    (set (filter neighbor? cells))))

(println "* neighbors")
;;
(println (= #{bottom-left top-right}
            (neighbors center [bottom-left center top-right])))
;;
(println (= #{center}
            (neighbors bottom-left [bottom-left center top-right])))

(defn kill-off
  "Removes cells that do not have enough neighbors to stay alive"
  [cells]
  (set (remove #(let [n_neighbors (count (neighbors % cells))]
                  (or (< n_neighbors 2)
                      (> n_neighbors 3)))
               cells)))

(println "* kill-off")

;; Cell with no neighbors dies
(println (= #{} (kill-off [center])))

;; Cell with one neighbor dies
(println (= #{} (kill-off [center top-left])))

;; Cell with two neighbors lives
(println (= #{center} (kill-off [center top-left top-right])))

;; Cell with three neighbors lives
(println (= #{center} (kill-off [center top-left top-right bottom-left])))

;; Cell with four neighbors dies
(println (= #{} (kill-off [center top-left top-right bottom-left bottom-right])))

(defn all-neighbors
  "Finds the cells in a 3x3 area around the given cell"
  [{x :x y :y}]
  (for [x-mod (range -1 2)
        y-mod (range -1 2)]
    {:x (+ x x-mod) :y (+ y y-mod)}))

(defn interesting-cells
  "Finds all cells that could potentially change on a given step"
  [cells]
  (reduce set/union (map all-neighbors cells)))

(defn fringe
  "Finds all cells that are currently dead but could come alive"
  [cells]
  (set/difference (set (interesting-cells cells))
                  cells))

(defn come-alive
  "Finds all cells that are currently dead but will come alive"
  [cells]
  (set (remove #(not (== 3 (count (neighbors % cells))))
               (fringe cells))))

(println "* come-alive")

;; Empty cell with no neighbors stays dead
(println (= #{} (come-alive [])))

;; Empty cell with one neighbor stays dead
(println (= #{} (come-alive [top-left])))

;; Empty cell with two neighbors stays dead
(println (= #{} (come-alive [top-left top-right])))

;; Empty cell with three neighbors comes alive
(println (= #{center} (come-alive [top-left top-right bottom-left])))

;; Empty cell with four neighbors stays dead
(println (= #{} (come-alive [top-left top-right bottom-left bottom-right])))

(defn step
  "Returns the cells that are still alive after or came alive during a single time step"
  [cells]
  (set/union (kill-off cells)
             (come-alive cells)))

;; Printing functions

;; Have a 21x21 window into the world
(def min-x -10)
(def max-x  10)
(def min-y -10)
(def max-y  10)

(defn print-cell [cell]
  (if cell
    (print "X")
    (print " ")))

(defn print-row [cells]
  (dorun (for [x (range min-x (inc max-x))]
    (print-cell (first (filter #(= x (% :x)) cells)))))
  (println))

(defn print-cells [cells]
  (println "---")
  (dorun (for [y (reverse (range min-y (inc max-y)))]
           (print-row (filter #(= y (% :y)) cells))))
  (println "---"))

;; Two mildly-interesting start states

(def blinker [{:x -1 :y 0} {:x 0 :y 0} {:x 1 :y 0}])
(def glider [{:x 2 :y 2} {:x 2 :y 1} {:x 2 :y 0} {:x 1 :y 0} {:x 0 :y 1}])

(def initial-state glider)

(def life (iterate step initial-state))

(dotimes [i 50]
  (print-cells (nth life i)))
