class Peg {
    constructor(riddle, additionalActionWhenTurnRight) {
        this.index = 0;
        this.values = [1, 2, 3, 4];
        this.riddle = riddle;
        this.additionalActionWhenTurnRight = additionalActionWhenTurnRight;
        this.turnLeftByItself = function () {
            if (this.index > 0) {
                this.index--;
            }
            else {
                this.index = this.values.length - 1;
            }
        };

        this.turnRightByItself = function () {
            if (this.index < this.values.length - 1) {
                this.index++;
            }
            else {
                this.index = 0;
            }
        };

        this.turnRightManually = function () {
            this.turnRightByItself();
            this.additionalActionWhenTurnRight(riddle);
        }
    }
}

class Riddle {

    constructor(initialStates, finalStates) {
        this.pegs = new Array();
        this.pegs.push(new Peg(this, function(riddle) { riddle.pegs[1].turnLeftByItself(); riddle.pegs[2].turnRightByItself(); }));
        this.pegs.push(new Peg(this, function(riddle) { riddle.pegs[0].turnRightByItself(); riddle.pegs[4].turnRightByItself(); }));
        this.pegs.push(new Peg(this, function(riddle) { riddle.pegs[1].turnLeftByItself(); riddle.pegs[3].turnLeftByItself(); }));
        this.pegs.push(new Peg(this, function(riddle) { riddle.pegs[0].turnLeftByItself(); riddle.pegs[4].turnRightByItself(); }));
        this.pegs.push(new Peg(this, function(riddle) { riddle.pegs[2].turnLeftByItself(); riddle.pegs[3].turnRightByItself(); }));

        this.reset = function() {
            this.initialStates.forEach((x, i) => { 
                if (this.pegs[i].values.includes(x)) {
                    this.pegs[i].index = x; 
                }
            });
        }

        if (initialStates instanceof Array) {
            this.initialStates = initialStates;
            this.reset();
        }

        if (finalStates instanceof Array) {
            this.finalStates = finalStates;
        }

        this.checkFinalState = function() {
            var ret = this.finalStates instanceof Array 
               && this.finalStates.length == 5
               && this.pegs.length == 5 
               && this.pegs[0].index == this.finalStates[0]
               && this.pegs[1].index == this.finalStates[1]
               && this.pegs[2].index == this.finalStates[2]
               && this.pegs[3].index == this.finalStates[3]
               && this.pegs[4].index == this.finalStates[4];
            
            if (ret) {
                console.log("Reached final state!");
            }
            return ret;
        }

        this.print = function() {
            var output = "[";
            this.pegs.forEach((x, i) => { 
                output += x.index + ", "; 
            });
            output = output.slice(0, -2);
            output += "]";
            return output;
        }
    }
}

var incrementElement = function(myarray, index) {
    const maxValue = 4;
    if (index < myarray.length) {
        if (myarray[index] < maxValue) {
            myarray[index]++;
        }
        else {
            myarray[index] = 0;
            incrementElement(myarray, index+1);
        }
    }
    else {
        throw "reached max index";
    }
}

var mutate = function(turns) {
    if (turns instanceof Array) {
        incrementElement(turns, 0);
    }
}

var i = 0, currentTry = 0;
const maxTries = 10000000;
var turns = [0, 0, 0, 0, 0]
var riddle = new Riddle([2, 4, 2, 1, 3], [2, 2, 4, 2, 2]);
try {
    while (!riddle.checkFinalState() && currentTry < maxTries) {
        riddle.reset();
        // console.log("Initial riddle state: " + riddle.print());

        for (var i = 0; i<turns.length; i++) {
            riddle.pegs[turns[i]].turnRightManually();
            // console.log(riddle.print());
        }

        mutate(turns);
        console.log("Current turns : " + turns);
        currentTry++;
    }
}
catch (ex) {
    console.error(ex);
}
finally {
    console.log("Current turns : " + turns);
}

if (currentTry == maxTries) {
    console.error("MaxTries reached, didn't find solution");
}