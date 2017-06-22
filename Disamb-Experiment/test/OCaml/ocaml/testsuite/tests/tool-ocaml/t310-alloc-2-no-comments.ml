open Lib;;
let v = Array.make 200000 2 in
let t = ref 0 in
Array.iter (fun x -> t := !t + x) v;
if !t <> 400000 then raise Not_found
;;