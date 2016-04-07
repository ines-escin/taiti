package similarityAnalyser.text

import groovy.util.logging.Slf4j
import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.linear.RealVector
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.Terms
import org.apache.lucene.index.TermsEnum
import org.apache.lucene.util.BytesRef
import taskAnalyser.Task

@Slf4j
class TextualSimilarityAnalyser {
    IndexManager indexManager
    IndexReader reader
    Set terms

    private configureIndexManager(Task task1, Task task2){
        indexManager = new IndexManager()
        indexManager.index(task1.acceptanceTests)
        indexManager.index(task2.acceptanceTests)
    }

    private configureIndexManager(String task1, String task2){
        indexManager = new IndexManager()
        indexManager.index(task1)
        indexManager.index(task2)
    }

    private getTermFrequencies(int docId){
        def frequencies = [:]
        Terms vector = reader.getTermVector(docId, "content")
        if(!vector) return frequencies
        TermsEnum termsEnum = null
        termsEnum = vector.iterator(termsEnum)

        BytesRef text
        while ((text = termsEnum?.next()) != null) {
            String term = text.utf8ToString()
            int freq = (int) termsEnum.totalTermFreq()
            frequencies += [(term):freq]
            terms += term
        }
        frequencies
    }

    private RealVector toRealVector(Map map) {
        RealVector vector = new ArrayRealVector(terms.size())
        int i = 0
        terms.each{ term ->
            int value = map.containsKey(term) ? map.get(term) : 0
            vector.setEntry(i++, value)
        }
        return (RealVector) vector.mapDivide(vector.getL1Norm())
    }

    private static double getCosineSimilarity(RealVector v1, RealVector v2){
        v1.dotProduct(v2) / (v1.norm * v2.norm)
    }

    double calculateSimilarity(Task task1, Task task2){
        terms = [] as Set
        configureIndexManager(task1, task2)
        reader = DirectoryReader.open(indexManager.indexDirectory)

        def freqVectorTask1 = getTermFrequencies(0).sort()
        //println "vector1: $freqVectorTask1"
        def freqVectorTask2 = getTermFrequencies(1).sort()
        //println "vector2: $freqVectorTask2"

        RealVector v1 = toRealVector(freqVectorTask1)
        RealVector v2 = toRealVector(freqVectorTask2)

        getCosineSimilarity(v1, v2)
    }

    double calculateSimilarity(String task1, String task2){
        if(task1=="" || task2=="") return 0
        terms = [] as Set
        configureIndexManager(task1, task2)
        reader = DirectoryReader.open(indexManager.indexDirectory)

        def freqVectorTask1 = getTermFrequencies(0).sort()
        log.info "vector1: $freqVectorTask1"
        def freqVectorTask2 = getTermFrequencies(1).sort()
        log.info "vector2: $freqVectorTask2"

        RealVector v1 = toRealVector(freqVectorTask1)
        RealVector v2 = toRealVector(freqVectorTask2)

        getCosineSimilarity(v1, v2)
    }

}