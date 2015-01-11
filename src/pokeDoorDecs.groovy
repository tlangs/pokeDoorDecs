
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

class Person {

    String name
    BufferedImage sprite
    BufferedImage image
    String pokemon

    Person(String name) {
        this.name = name
    }
}

def generations = [kanto: 1..151, johto: 152..251, hoenn: 252..386,
                   sinnoh: 387..493, unova: 494..649, kalos: 650..720]

def cli = new CliBuilder(usage: "pokeDoorDecs.groovy [-h] [-d path] [-r regions] [file ...]")
// Create the list of options.
cli.with {
    h longOpt: "help", "Display the help screen"
    d longOpt: "directory", args: 1, argName: "directory", "Define an output directory"
    r longOpt: "regions", args: 1, argName: "regions", "Define which regions to use. Ex: \"johto,hoenn,kanto\""

}

def options = cli.parse(args)

if (!options) {
    cli.usage()
    System.exit(0)
}

if (options.h) {
    println "pokeDoorDecs makes pictures with names and pokemon. \n" +
            "It expects a well formatted list of JSON object, with each object having a name. \n" +
            "A pokemon can be provided, which will be matched by name ignoring case. \n" +
            "If no pokemon is provided, then a random pokemon pokemon will be chosen. \n" +
            "By default, all pokemon are available to be randomly selected. \n" +
            "This can be changed by adding the -r flag and specifying which regions to use, \n" +
            "separated by commas. \n\n" +
            "Images are written to the current working directory, unless a directory is \n" +
            "provided using the -d flag. Any existing images with the same name will be \n" +
            "overwritten. \n\n" +
            "An example of a JSON list accepted follows:\n\n" +
            "[\n" +
            "\t{\n" +
            "\t\t\"name\": \"Trevyn Langsford\",\n" +
            "\t\t\"pokemon\": \"arcanine\"\n" +
            "\t},\n" +
            "\t{\n" +
            "\t\t\"name\": \"Dwight Clarke\"\n" +
            "\t}\n" +
            "]\n\n" +
            "Calling 'groovy pokeDoorDecs.groovy -r kanto,johto file.json' where file.json \n" +
            "contains the example JSON list would produce a door dec for Trevyn with \n" +
            "Arcanine and choose a random pokemon from the Johto region for Dwight."
    System.exit(0)
}

def file
try {
    def extraArgs = options.arguments()
    if (extraArgs) {
        file = new File(options.arguments()[0])
        if (!file.exists()) {
            throw new FileNotFoundException("The file was not found.")
        }
    } else {
        cli.usage()
        System.exit(0)
    }
} catch(Exception e) {
    println e.getMessage()
    System.exit(0)
}



def slurper = new JsonSlurper()
def names = slurper.parse(file)
def people = []

names.each { obj ->
    def person = new Person(obj.name)
    person.pokemon = obj.pokemon
    people.add(person)
}

println "Parsed ${people.size()} names."

def pokedex = slurper.parseText(new URL("http://pokeapi.co/api/v1/pokedex/1").text)
people.each { person ->
    try {
        def ids = []
        def regions
        if (options.r) {
            regions = options.regions.split(",")
            regions.each {region ->
                ids << generations["${region}"]
            }
        } else {
            generations.each{region ->
                ids << region.value
            }
        }

        ids = ids.flatten()

        def mon = [:]
        if (!person.pokemon) {
            mon.resource_uri = "api/v1/pokemon/${ids[(Integer) Math.floor(Math.random() * ids.size())]}/"
        } else {
            mon = pokedex["pokemon"].find {a -> a.name.equalsIgnoreCase(person.pokemon)}
        }

        def info = slurper.parseText(new URL("http://pokeapi.co/${mon.resource_uri}").text)
        def spriteInfo = slurper.parseText(new URL("http://pokeapi.co/${info.sprites[0].resource_uri}").text)
        person.sprite = ImageIO.read(new URL("http://pokeapi.co/${spriteInfo.image}"))
        println "Found image for ${person.name}."

    } catch (IOException e) {
        println "Could not get image from URL"
        println e.getMessage();
    }
}

people.each { person ->
    def image = new BufferedImage(
            (person.sprite.getWidth() * 3) + 50 ,(person.sprite.getHeight() * 3) + 70,
            BufferedImage.TYPE_INT_ARGB)
    def g = (Graphics2D)image.getGraphics()

    def font = Font.createFont(Font.TRUETYPE_FONT, new File("fonts/PokemonSolid.ttf"))
    float size = 120F
    g.setFont(font.deriveFont(size))
    FontMetrics fontMetrics = g.getFontMetrics()
    while (fontMetrics.stringWidth(person.name) > person.sprite.getWidth() * 3 - 50) {
        size--
        g.setFont(font.deriveFont(size))
        fontMetrics = g.getFontMetrics()
    }

    g.setColor(Color.WHITE)
    g.fillRect(0, 0, image.getWidth(), image.getHeight())
    g.drawImage(person.sprite, 25, 0, person.sprite.getWidth() * 3 , person.sprite.getHeight() * 3, null)

    g.setColor(Color.BLACK)
    g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_GASP)

    def fontht = fontMetrics.getHeight()
    g.drawString(person.name, 50, (person.sprite.getHeight() * 3) + fontht/4)

    person.image = image
}

println "Retrieved pokemon sprites."

def path = "."
if (options.d) {
    path = options.directory
}
def dir = new File(path)
if (!dir.exists()) {
    dir.mkdir()
}

people.each { person  ->
    ImageIO.write(person.image, "png", new File("${path}/${person.name}.png"))
}

println "Done! Processed ${people.size()} people."
