open Lib;;
let rec f n =
  if n <= 0 then []
  else n :: f (n-1)
in
Gc.compact ();
let l = f 300 in
if List.fold_left (+) 0 l <> 301 * 150 then raise Not_found
;;