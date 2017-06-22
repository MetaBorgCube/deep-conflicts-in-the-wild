let suites :  Mt.pair_suites ref  = ref []
let test_id = ref 0
let eq loc x y = 
  incr test_id ; 
  suites := 
    (loc ^" id " ^ (string_of_int !test_id), (fun _ -> Mt.Eq(x,y))) :: !suites


let u () = "xx %s" ^^ "yy"

let () = 
  eq __LOC__ (Format.asprintf (u ()) "x") ("xx x" ^ "yy")

let () = Mt.from_pair_suites __FILE__ !suites
