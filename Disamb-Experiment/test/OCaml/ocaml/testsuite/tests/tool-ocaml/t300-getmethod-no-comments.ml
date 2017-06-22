open Lib;;

class c = object
  method m = 23
end;;

let o = new c in
if o#m <> 23 then raise Not_found
;;
