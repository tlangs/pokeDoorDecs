@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7.2')
import groovy.json.*
import groovyx.net.http.HTTPBuilder
import org.fusesource.jansi.Ansi

import java.awt.Color
import java.awt.Graphics
import java.awt.image.BufferedImage
import javax.imageio.*
import java.awt.Font

class Resident {

    String name
    BufferedImage sprite
    BufferedImage image
    Integer num

    Resident(String name) {
        this.name = name
    }
}

def http = new HTTPBuilder("http://pokeapi.co/")
def names = new JsonSlurper().parse(new File("residents.json"))
def residents = []

names.each { name ->
    def resident = new Resident(name)
    def num = Math.random() * 151
    num = (Integer) Math.ceil(num)
    resident.num = num
    residents.add(resident)
}

println "Parsed ${residents.size()} residents."

residents.each { resident ->
    try {
        http.get(path: "api/v1/pokemon/${resident.num}/",) { resp, info ->

            http.get(path: "api/v1/sprite/${resident.num}") { resp2, spriteInfo ->
                def imgURL = spriteInfo["image"]
                def url = new URL("http://pokeapi.co/${imgURL}")
                resident.sprite = ImageIO.read(url)

                println "Found image for ${resident.name}."
            }
        }
    } catch (IOException e) {
        println "Could not get image from URL"
        println e.getMessage();
    }
}

residents.each { resident ->
    def image = new BufferedImage((resident.sprite.getWidth() * 3) + 50 ,(resident.sprite.getHeight() * 3) + 70, BufferedImage.TYPE_INT_ARGB)
    def g = image.getGraphics()
    g.setFont(new Font ("Garamond", Font.ITALIC , 40))

    g.drawImage(resident.sprite, 25, 0, resident.sprite.getWidth() * 3 , resident.sprite.getHeight() * 3, null)
    g.setColor(Color.BLACK)
    g.drawString(resident.name, 50, (resident.sprite.getHeight() * 3) + 10)

    resident.image = image
}

println "Retrieved pokemon sprites."

def dir = new File("decs")
if (!dir.exists()) {
    dir.mkdir()
}
dir.eachFile { file ->
    file.delete()
}

residents.each { resident  ->
    ImageIO.write(resident.image, "png", new File("decs/${resident.name}.png"))
}


println "Done! Processed ${residents.size()} residents."
