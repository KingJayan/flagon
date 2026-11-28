import"./modulepreload-polyfill-B5Qt9EMX.js";import{L as j,s as A}from"./log-Dx_juzxH.js";const f=[{name:"Hello",code:`public class Main {
    public static void main(String[] args) {
        System.out.println("Hello from Java, compiled in your browser.");
    }
}
`},{name:"Records and streams",code:`import java.util.Comparator;
import java.util.List;

public class Main {
    record Planet(String name, double au) {}

    public static void main(String[] args) {
        var planets = List.of(
                new Planet("Mercury", 0.39), new Planet("Venus", 0.72), new Planet("Earth", 1.0),
                new Planet("Mars", 1.52), new Planet("Jupiter", 5.2));
        planets.stream()
                .filter(planet -> planet.au() > 0.5)
                .sorted(Comparator.comparingDouble(Planet::au).reversed())
                .forEach(planet -> System.out.println(planet.name() + " " + planet.au() + " AU"));
    }
}
`},{name:"Sealed types and switch",code:`public class Main {
    sealed interface Shape permits Circle, Square {}

    record Circle(double radius) implements Shape {}

    record Square(double side) implements Shape {}

    static double area(Shape shape) {
        return switch (shape) {
            case Circle circle -> Math.PI * circle.radius() * circle.radius();
            case Square square -> square.side() * square.side();
        };
    }

    public static void main(String[] args) {
        for (Shape shape : new Shape[] {new Circle(1), new Square(2)}) {
            System.out.println(shape + " has area " + area(shape));
        }
    }
}
`},{name:"Threads",code:`public class Main {
    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            for (int tick = 1; tick <= 3; tick++) {
                System.out.println("worker tick " + tick);
                try {
                    Thread.sleep(250);
                } catch (InterruptedException stopped) {
                    return;
                }
            }
        });
        worker.start();
        System.out.println("main waits for the worker");
        worker.join();
        System.out.println("worker done");
    }
}
`}],k="/flagon/playground/",L=64*1024,T=1e4,g=2e3,$="flagon:playground:source",h="__FlagonRun",U=`final class ${h} {
    public static void main(String[] args) throws InterruptedException {
        int code = 0;
        try {
            Main.main(args);
        } catch (Throwable thrown) {
            System.err.print("Exception in thread \\"main\\" ");
            thrown.printStackTrace();
            code = 1;
        }
        // a started thread counts once it first runs, so wait a turn before counting
        do {
            Thread.sleep(10);
        } while (Thread.activeCount() > 1);
        System.exit(code);
    }
}
`,_=document.querySelector("#app"),M=document.querySelector("#state"),t=document.querySelector("#code"),S=document.querySelector("#gutter"),x=document.querySelector("#run"),I=document.querySelector("#stop"),m=document.querySelector("#example"),N=document.querySelector("#problems"),c=new j(document.querySelector("#log"),g);let b="loading",d,s,v=0,w=0,C=0,q=0;function l(e){b=e,_.dataset.state=e,M.textContent=e,M.setAttribute("aria-label",`state: ${e}`),x.disabled=e==="loading"||e==="compiling",I.disabled=e!=="running"}function u(e){c.append(e,"error"),l("failed")}function K(){try{return localStorage.getItem($)??f[0].code}catch{return f[0].code}}function y(){try{localStorage.setItem($,t.value)}catch{}}function E(){const e=t.value.split(`
`).length;q!==e&&(q=e,S.textContent=Array.from({length:e},(r,n)=>n+1).join(`
`)),S.scrollTop=t.scrollTop}function R(e){t.value=e,E(),y()}function B(e){const r=t.value.split(`
`),n=r.slice(0,e-1).reduce((i,a)=>i+a.length+1,0);t.focus(),t.setSelectionRange(n,n+(r[e-1]?.length??0))}function D(e,r,n,i){const a=document.createElement("li"),o=document.createElement("button");o.type="button",o.className=i?"error":"warn",o.textContent=`${e}:${r} ${n}`,o.addEventListener("click",()=>B(e)),a.append(o),N.append(a)}function p(e){s&&(s.terminate(),s=void 0,e&&c.append(e,"warn"),l("exited"))}function H(e){l("running"),w=0;const r=new Worker(new URL("/assets/program.worker-CK7Ql-9z.js",import.meta.url),{type:"module"});s=r;const n=v=window.setTimeout(()=>p(`stopped after ${T/1e3} s`),T);r.addEventListener("message",i=>{if(s!==r)return;const a=i.data;if(a.kind==="lines"){for(const o of a.lines)c.append(o.text,o.error?"error":void 0);w+=a.lines.length,w>=g&&(window.clearTimeout(n),p(`stopped after ${g} lines`))}else a.kind==="exited"?(window.clearTimeout(n),p(a.code===0?void 0:`exit code ${a.code}`)):(window.clearTimeout(n),s=void 0,r.terminate(),u(a.message))}),r.addEventListener("error",i=>{window.clearTimeout(n),s=void 0,u(i.message||"the program could not start")}),r.postMessage({code:e.slice().buffer},[])}function P(){if(!(!d||b==="loading"||b==="compiling")){if(t.value.length>L){u(`the source is over ${L/1024} KB`);return}window.clearTimeout(v),p(),c.clear(),N.replaceChildren(),l("compiling"),C+=1,d.postMessage({id:String(C),command:"compile",files:[{path:"Main.java",content:t.value},{path:`${h}.java`,content:U}],options:{outputName:"app",mainClass:h}},[])}}function O(e){switch(e.command){case"initialized":d.postMessage({id:"classlib",command:"load-classlib",url:`${k}compile-classlib-teavm.bin`,runtimeUrl:`${k}runtime-classlib-teavm.bin`},[]);return;case"ok":l("ready");return;case"error":u(e.text);return;case"compiler-diagnostic":case"diagnostic":{const r=e.severity.toLowerCase()==="error";if(e.command==="compiler-diagnostic"&&e.fileName.includes(h)){c.append("Main.java needs a class named Main with a main method","error");return}const n=e.command==="compiler-diagnostic"?e.columnNumber:0;e.lineNumber>0&&D(e.lineNumber,n,e.message,r),c.append(e.lineNumber>0?`Main.java:${e.lineNumber}: ${e.message}`:e.message,r?"error":"warn");return}case"compilation-complete":e.status==="successful"&&e.script?H(e.script):l("failed");return}}function W(){if(!A()){u("this browser has no WASM-GC support, which the compiler needs");return}c.append("loading javac and TeaVM"),d=new Worker(new URL("/assets/compiler.worker-b4qRIsJ7.js",import.meta.url),{type:"module"}),d.addEventListener("message",e=>O(e.data)),d.addEventListener("error",e=>u(e.message||"the compiler could not start"))}for(const[e,r]of f.entries())m.add(new Option(r.name,String(e)));m.addEventListener("change",()=>{const e=f[Number(m.value)];e&&R(e.code),m.value=""});t.addEventListener("input",()=>{E(),y()});t.addEventListener("scroll",()=>{S.scrollTop=t.scrollTop});t.addEventListener("keydown",e=>{(e.ctrlKey||e.metaKey)&&e.key==="Enter"?(e.preventDefault(),P()):e.key==="Tab"&&!e.shiftKey&&!t.dataset.leaving?(e.preventDefault(),t.setRangeText("    ",t.selectionStart,t.selectionEnd,"end"),E(),y()):e.key==="Escape"&&(t.dataset.leaving="true")});t.addEventListener("blur",()=>delete t.dataset.leaving);t.addEventListener("keyup",e=>{e.key!=="Escape"&&e.key!=="Tab"&&delete t.dataset.leaving});x.addEventListener("click",P);I.addEventListener("click",()=>{window.clearTimeout(v),p("stopped")});R(K());l("loading");W();
//# sourceMappingURL=playground-U0Fl4Oe9.js.map
