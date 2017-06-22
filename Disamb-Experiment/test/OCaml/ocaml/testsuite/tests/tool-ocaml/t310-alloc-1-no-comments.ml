open Lib;;
let rec f a n =
  if n <= 0 then a
  else f (1::a) (n-1)
in
let l = f [] 30000 in
if List.fold_left (+) 0 l <> 30000 then raise Not_found
;;