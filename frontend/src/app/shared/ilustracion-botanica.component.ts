import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Ilustración de línea fina (helecho, orquídea y rama de café, flora del Quindío) que se
 * dibuja una sola vez al cargar, como el stencil que el artista calca antes de tatuar.
 */
@Component({
  selector: 'hg-ilustracion-botanica',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg viewBox="0 0 420 520" role="img" aria-label="Ilustración botánica de línea fina">
      <g class="trazo" fill="none" stroke-linecap="round" stroke-linejoin="round">
        <!-- tallo principal -->
        <path class="t t1" d="M210 500 C205 430 214 360 206 290 C199 228 212 170 236 120 C252 88 270 66 290 50"/>
        <!-- helecho izquierdo -->
        <path class="t t2" d="M207 330 Q140 300 70 180"/>
        <path class="t t3" d="M193.6 323.1 Q204.3 307.8 194.9 291.7 Q184.2 307.0 193.6 323.1 M193.6 323.1 Q174.9 322.8 166.8 339.6 Q185.5 339.9 193.6 323.1 M180.1 314.4 Q191.2 301.3 183.9 285.8 Q172.8 298.9 180.1 314.4 M180.1 314.4 Q163.0 312.5 154.2 327.2 Q171.2 329.1 180.1 314.4 M166.5 303.9 Q177.6 292.8 172.1 278.1 Q161.1 289.2 166.5 303.9 M166.5 303.9 Q151.2 301.0 142.1 313.7 Q157.4 316.6 166.5 303.9 M152.9 291.6 Q163.6 282.3 159.6 268.8 Q148.9 278.0 152.9 291.6 M152.9 291.6 Q139.3 288.0 130.3 298.9 Q143.9 302.5 152.9 291.6 M139.2 277.5 Q149.2 269.8 146.4 257.5 Q136.4 265.2 139.2 277.5 M139.2 277.5 Q127.3 273.5 118.7 282.7 Q130.6 286.7 139.2 277.5 M125.5 261.6 Q134.7 255.3 132.8 244.4 Q123.6 250.7 125.5 261.6 M125.5 261.6 Q115.2 257.5 107.2 265.2 Q117.5 269.3 125.5 261.6 M111.7 243.9 Q119.9 238.8 118.7 229.3 Q110.5 234.4 111.7 243.9 M111.7 243.9 Q103.0 240.0 95.8 246.3 Q104.5 250.2 111.7 243.9 M97.9 224.4 Q104.9 220.4 104.2 212.4 Q97.2 216.4 97.9 224.4 M97.9 224.4 Q90.7 220.8 84.4 225.8 Q91.6 229.5 97.9 224.4 M84.0 203.1 Q89.8 200.1 89.5 193.5 Q83.7 196.5 84.0 203.1 M84.0 203.1 Q78.2 200.0 72.9 203.9 Q78.7 207.0 84.0 203.1"/>
        <!-- helecho derecho -->
        <path class="t t2" d="M210 250 Q270 230 330 170"/>
        <path class="t t3" d="M227.1 243.5 Q231.2 255.5 243.9 256.8 Q239.8 244.8 227.1 243.5 M227.1 243.5 Q234.8 233.3 228.9 222.1 Q221.2 232.2 227.1 243.5 M244.3 235.3 Q248.7 245.6 259.9 245.9 Q255.5 235.6 244.3 235.3 M244.3 235.3 Q250.4 225.9 244.4 216.4 Q238.3 225.9 244.3 235.3 M261.4 225.5 Q265.9 234.1 275.5 233.7 Q271.1 225.1 261.4 225.5 M261.4 225.5 Q266.1 217.0 260.3 209.3 Q255.7 217.7 261.4 225.5 M278.6 214.1 Q282.8 221.1 290.9 220.2 Q286.7 213.2 278.6 214.1 M278.6 214.1 Q282.0 206.7 276.7 200.5 Q273.3 207.9 278.6 214.1 M295.7 201.0 Q299.5 206.5 306.0 205.4 Q302.2 199.9 295.7 201.0 M295.7 201.0 Q298.1 194.9 293.6 190.1 Q291.1 196.2 295.7 201.0 M312.9 186.3 Q316.0 190.4 320.9 189.2 Q317.8 185.2 312.9 186.3 M312.9 186.3 Q314.5 181.5 310.7 178.0 Q309.1 182.8 312.9 186.3"/>
        <!-- hojas de café -->
        <path class="t t4" d="M210 400 C244 392 274 372 292 344 C262 344 232 362 210 400 Z"/>
        <path class="t t4" d="M212 380 C236 372 258 360 274 352"/>
        <path class="t t4" d="M206 440 C176 432 150 414 136 390 C162 390 188 406 206 440 Z"/>
        <path class="t t4" d="M205 424 C186 414 166 404 152 398"/>
        <!-- frutos -->
        <circle class="t t5" cx="232" cy="414" r="7"/>
        <circle class="t t5" cx="246" cy="424" r="6"/>
        <circle class="t t5" cx="230" cy="430" r="5"/>
        <!-- orquídea -->
        <path class="t t6" d="M290 50 C300 70 300 92 288 108"/>
        <path class="t t6" d="M288 108 C262 96 240 104 232 124 C256 132 276 126 288 108 Z"/>
        <path class="t t6" d="M288 108 C312 92 336 96 346 116 C324 126 302 124 288 108 Z"/>
        <path class="t t6" d="M288 108 C278 128 280 152 296 166 C308 150 304 126 288 108 Z"/>
        <path class="t t6" d="M288 108 C292 84 312 70 334 74 C330 96 312 108 288 108 Z"/>
        <path class="t t6" d="M288 108 C270 86 248 80 232 88 C244 106 266 112 288 108 Z"/>
        <path class="t t7" d="M288 108 m-6 6 a8 8 0 1 0 12 0"/>
        <!-- puntillismo -->
        <g class="puntos">
          <circle cx="300" cy="140" r="1.3"/><circle cx="306" cy="146" r="1.1"/><circle cx="296" cy="150" r="1"/>
          <circle cx="252" cy="118" r="1.2"/><circle cx="246" cy="124" r="1"/><circle cx="320" cy="112" r="1.1"/>
          <circle cx="270" cy="360" r="1.2"/><circle cx="262" cy="366" r="1"/><circle cx="160" cy="404" r="1.1"/>
          <circle cx="168" cy="410" r="1"/><circle cx="120" cy="270" r="1"/><circle cx="96" cy="250" r="1"/>
        </g>
      </g>
    </svg>
  `,
  styles: [`
    :host { display: block; }
    svg { width: 100%; height: auto; overflow: visible; }
    .trazo { stroke: var(--tinta); stroke-width: 1.4; }
    .t {
      stroke-dasharray: 2400; stroke-dashoffset: 2400;
      animation: dibujar 2.6s cubic-bezier(.6,.05,.3,1) forwards;
    }
    .t1 { animation-delay: .1s; }
    .t2 { animation-delay: .7s; }
    .t3 { animation-delay: .9s; animation-duration: 3.2s; stroke-width: .9; stroke: var(--grafito); stroke-dasharray: 6000; stroke-dashoffset: 6000; }
    .t4 { animation-delay: 1.2s; }
    .t5 { animation-delay: 1.7s; stroke: var(--sepia); }
    .t6 { animation-delay: 1.4s; }
    .t7 { animation-delay: 2s; stroke: var(--laton); stroke-width: 1.8; }
    .puntos { fill: var(--grafito); stroke: none; opacity: 0; animation: asomar 1s ease 2.6s forwards; }
    @keyframes dibujar { to { stroke-dashoffset: 0; } }
    @keyframes asomar { to { opacity: .8; } }
    @media (prefers-reduced-motion: reduce) {
      .t { stroke-dashoffset: 0; animation: none; }
      .puntos { opacity: .8; animation: none; }
    }
  `]
})
export class IlustracionBotanicaComponent {}
