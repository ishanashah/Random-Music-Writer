import java.io.*;
import java.util.*;
import javax.sound.midi.*;

public class RandomMusicWriter {
    //String representation of input file
    private String input;
    //seed used to write MidiEvents
    private MidiMessage[] seed;

    private Sequence inputSequence;
    private Track[] inputTracks;
    private Track[] outputTracks;
    private Sequence outputSequence;

    //level of analysis
    private int level;
    private long previousTick;


    public static void main(String[] args)
            throws IOException, InvalidMidiDataException{
        //command line arguments
        String source = args[0];
        String result = args[1];
        int k = Integer.parseInt(args[2]);
        int length = Integer.parseInt(args[3]);

        //Validate command line arguments.
        if(k < 0 || length < 0){
            System.err.println("k or length are negative");
        }
        Sequence inputSequence = MidiSystem.getSequence(new File(source));
        Track[] inputTracks = inputSequence.getTracks();
        int min = inputTracks[0].size();
        for(int i = 1; i < inputTracks.length; i++){
            if(min > inputTracks[i].size()) {
                min = inputTracks[i].size();
            }
        }
        if(min <= k){
            System.err.println("Level of analysis is too large");
            System.err.println("Please input a level of analysis less than " + min);
        }

        //Create and use instance of music processor
        RandomMusicWriter rw = createProcessor(k);
        rw.readMusic(source);
        rw.writeMusic(result, length);
    }

    private static RandomMusicWriter createProcessor(int level) {
        return new RandomMusicWriter(level);
    }

    private RandomMusicWriter(int level) {
        this.seed = new MidiMessage[level];
        this.level = level;
        this.input = "";
    }

    //assign a Sequence representation of the input file to input
    private void readMusic(String inputFilename)
            throws IOException, InvalidMidiDataException {
        inputSequence = MidiSystem.getSequence(new File(inputFilename));
        inputTracks = inputSequence.getTracks();
        outputSequence = new Sequence(inputSequence.getDivisionType(),
                inputSequence.getResolution(),
                inputTracks.length);
        outputTracks = new Track[inputTracks.length];
        for(int i = 0; i < outputTracks.length; i++){
            outputTracks[i] = outputSequence.getTracks()[i];
        }
    }

    //create a new seed
    private void createSeed(int index){
        if(level == 0){
            return;
        }
        Track thisTrack = inputTracks[index];
        List<MidiMessage> theseEvents = new ArrayList<>();
        for(int i = 0; i < thisTrack.size(); i++){
            theseEvents.add(thisTrack.get(i).getMessage());
        }
        int beginIndex = (new Random()).nextInt(thisTrack.size() - level);
        for(int i = beginIndex; i < beginIndex + level; i++){
            seed[i - beginIndex] = thisTrack.get(i).getMessage();
        }
    }

    //write random music onto the output file
    private void writeMusic(String outputFilename, int length)
            throws IOException, InvalidMidiDataException {
        for(int i = 0; i < inputTracks.length; i++){
            createSeed(i);
            writeMusic(length, i);
        }
        for(int i = 0; i < outputTracks.length; i++){
            outputSequence.getTracks()[i] = outputTracks[i];
        }
        MidiSystem.write(outputSequence, 1, new File(outputFilename));
    }

    //helper method for writeMusic()
    private void writeMusic(int length, int index)
            throws IOException, InvalidMidiDataException {
        for(int i = 0; i < inputTracks[index].size(); i++){
            if(inputTracks[index].get(i).getTick() == (long) 0){
                outputTracks[index].add(inputTracks[index].get(i));
            } else {
                break;
            }
        }
        for(int i = 0; i < length; i++){
            MidiMessage output = getOutput(inputTracks[index], index);
            outputTracks[index].add(new MidiEvent(output, outputTracks[index].ticks() + previousTick));
        }
    }

    //check the equality of two MidiMessages
    private boolean compareMidiMessages(MidiMessage lhs, MidiMessage rhs){
        byte[] lhsMessage = lhs.getMessage();
        byte[] rhsMessage = rhs.getMessage();
        if(lhsMessage.length != rhsMessage.length){
            return false;
        }
        for(int i = 0; i < lhsMessage.length; i++){
            if(lhsMessage[i] != rhsMessage[i]){
                return false;
            }
        }
        return true;
    }

    //returns a random MidiMessage that follows the current seed
    private MidiMessage getOutput(Track inputTrack, int index){
        ArrayList<MidiEvent> output = new ArrayList<MidiEvent>();
        ArrayList<Integer> indexes = new ArrayList<Integer>();
        for(int i = 0; i < inputTrack.size() - seed.length; i++){
            if(inputTrack.get(i).getTick() != (long) 0){
                boolean isSeed = true;
                for(int j = 0; j < seed.length; j++){
                    if(!compareMidiMessages(seed[j], inputTrack.get(i).getMessage())){
                        isSeed = false;
                        break;
                    }
                    i++;
                }
                if(isSeed){
                    output.add(inputTrack.get(i));
                    indexes.add(i);
                }
            }
        }
        if(output.size() == 0){
            createSeed(index);
            return getOutput(inputTrack, index);
        }
        int randomIndex = new Random().nextInt(output.size());
        previousTick = 0;
        if(indexes.get(randomIndex) > 0){
            previousTick = inputTrack.get(indexes.get(randomIndex) - 1).getTick();
        }
        previousTick = inputTrack.get(indexes.get(randomIndex)).getTick() - previousTick;
        MidiMessage randomMessage = output.get(randomIndex).getMessage();
        if(level > 0) {
            updateSeed(randomMessage);
        }
        return randomMessage;
    }

    //updates seed to account for a new MidiMessage
    private void updateSeed(MidiMessage input){
        MidiMessage[] original = seed;
        seed = new MidiMessage[original.length];
        for(int i = 1; i < original.length; i++){
            seed[i - 1] = original[i];
        }
        seed[original.length - 1] = input;
    }
}