@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.0-RC2' )
import groovy.json.*
import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*

def http = new HTTPBuilder("http://pokeapi.co")

def pokemon = [:]

for (i in 1..151) {
    http.get(path: "api/v1/pokemon/${i}",) { resp, reader ->
         pokemon[i] = reader["name"]
    }
}

def sprites = [:]

pokemon.each { num, name -> 
    println "api/v1/sprite/${num}"
    http.get(path: "api/v1/sprite/${num}") { resp, reader -> 
        def imgURL = reader["image"]
        println imgURL
        
        http.get(path: imgURL) { resp2, reader2 ->
        def br = new BufferedReader(new InputStreamReader(reader2));
        while ((line = br 
            new File("sprites/${num}.png") << reader2
        }
    }
}

//sprites.each { k, v -> 
//    new File("sprites/${k}.png") << v
//}

def json = new JsonBuilder(pokemon)

def file = new File("pokemon.json")
file.write(json.toPrettyString())
println "Done! Got data for ${pokemon.size()} pokemon"
