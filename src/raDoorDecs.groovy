
/**
 * Created by trevyn on 1/7/15.
 */

@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7.2')
import groovy.json.*
import groovyx.net.http.HTTPBuilder

import java.awt.Color
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.imageio.*
import java.awt.Font

class RA {

    String name
    BufferedImage sprite
    BufferedImage image
    String pokemon

    RA(String name) {
        this.name = name
    }
}

def http = new HTTPBuilder("http://pokeapi.co/")
def names = new JsonSlurper().parse(new File("residentassistants.json"))
def residentsAssistants = []

names.each { obj ->
    def ra = new RA(obj.name)
    ra.pokemon = obj.pokemon
    residentsAssistants.add(ra)
}

println "Parsed ${residentsAssistants.size()} names."

residentsAssistants.each { resident ->
    try {
        def pokedex = new JsonSlurper().parseText(new URL("http://pokeapi.co/api/v1/pokedex/1").text)
        def mon = pokedex["pokemon"].find {a -> a.name == resident.pokemon}
        http.get(path: mon.resource_uri) { resp, info ->
            http.get(path: info.sprites[0].resource_uri) { resp2, spriteInfo ->
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

residentsAssistants.each { resident ->
    def image = new BufferedImage((resident.sprite.getWidth() * 3) + 50 ,(resident.sprite.getHeight() * 3) + 70, BufferedImage.TYPE_INT_ARGB)
    def g = (Graphics2D)image.getGraphics()
    float size = 120F
    def font = Font.createFont(Font.TRUETYPE_FONT, new File("fonts/PokemonSolid.ttf"))
    g.setFont(font.deriveFont(size))
    FontMetrics fontMetrics = g.getFontMetrics()
    while (fontMetrics.stringWidth(resident.name) > resident.sprite.getWidth() * 3 - 50) {
        size--
        g.setFont(font.deriveFont(size))
        fontMetrics = g.getFontMetrics()
    }

    g.setColor(Color.WHITE)
    g.fillRect(0, 0, image.getWidth(), image.getHeight())

    g.drawImage(resident.sprite, 25, 0, resident.sprite.getWidth() * 3 , resident.sprite.getHeight() * 3, null)

    g.setColor(Color.BLACK)
    g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_GASP)

    def fontht = fontMetrics.getHeight()
    g.drawString(resident.name, 50, (resident.sprite.getHeight() * 3) + fontht/4)

    resident.image = image
}

println "Retrieved pokemon sprites."

def dir = new File("radecs")
if (!dir.exists()) {
    dir.mkdir()
}
dir.eachFile { file ->
    file.delete()
}

residentsAssistants.each { resident  ->
    ImageIO.write(resident.image, "png", new File("radecs/${resident.name}.png"))
}

println "Done! Processed ${residentsAssistants.size()} residents."
