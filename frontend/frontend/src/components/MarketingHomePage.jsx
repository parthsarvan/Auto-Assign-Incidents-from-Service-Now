import React from 'react';
import { Link } from 'react-router-dom';
import { getCurrentUser } from '../services/auth';
import { getDefaultRouteForUser } from '../services/permissions';
import './MarketingHomePage.css';

const comparisonRows = [
  {
    category: 'Team-aware ServiceNow routing',
    incteam: 'Built into setup and ownership mapping.',
    traditional: 'Often spread across several modules or custom process work.',
  },
  {
    category: 'Organization-first onboarding',
    incteam: 'Create an org, connect ServiceNow, then onboard teams cleanly.',
    traditional: 'Usually designed around larger platform setup before team-level ownership.',
  },
  {
    category: 'Manager handoff model',
    incteam: 'Org admins can invite team managers before setup is finished.',
    traditional: 'Role setup is often possible, but rarely shaped around staged incident-team rollout.',
  },
  {
    category: 'Operational visibility',
    incteam: 'Availability, mapping, diagnostics, and logs stay in one focused workspace.',
    traditional: 'Capabilities may exist, but teams often jump across multiple surfaces to get context.',
  },
];

const featureHighlights = [
  {
    eyebrow: 'CI-User mapping',
    title: 'InciTeam routes incidents without manual triage.',
    description:
      'Map configuration items to the right owners so incident assignment happens automatically instead of depending on someone to manually decide where each incident should go.',
  },
  {
    eyebrow: 'Leaves and breaks',
    title: 'Coverage stays smart when someone is away.',
    description:
      'If a teammate is on leave or currently on break, InciTeam avoids assigning new incidents to them and keeps routing aligned with the people who are actually available.',
  },
  {
    eyebrow: 'Schedules',
    title: 'Plan shifts clearly, including exceptions like holidays.',
    description:
      'Managers can define who is on shift, when they are covering, and adjust schedules when special calendars or holiday rotations need something different.',
  },
  {
    eyebrow: 'Dashboard',
    title: 'See team coverage day by day or week by week.',
    description:
      'The dashboard gives a clear view of who is on shift, who is on leave, and who is on break so teams can understand current and upcoming coverage fast.',
  },
  {
    eyebrow: 'Logs',
    title: 'Understand what happened to every incident.',
    description:
      'Operational logs show whether an incident was assigned, skipped, or blocked, giving teams a simple timeline of what the system actually did.',
  },
  {
    eyebrow: 'Diagnostics',
    title: 'Explain why InciTeam chose a particular owner.',
    description:
      'Diagnostics help teams understand the routing logic behind each assignment so managers can trust the decision path and refine setup when needed.',
  },
];

export default function MarketingHomePage() {
  const currentUser = getCurrentUser();
  const primaryPath = currentUser ? getDefaultRouteForUser(currentUser) : '/signup';
  const primaryLabel = currentUser ? 'Open App' : 'Create Organization';
  const secondaryPath = currentUser ? '/summary' : '/signin';
  const secondaryLabel = currentUser ? 'Open Summary' : 'Sign In';
  const authLinkProps = currentUser ? {} : { target: '_blank', rel: 'noreferrer' };

  return (
    <div className="marketing-page">
      <header className="marketing-nav">
        <div className="marketing-nav__brand">
          <div className="marketing-nav__eyebrow">Incident and Team Management Tool for Service Now</div>
          <div className="marketing-nav__name">InciTeam</div>
        </div>
        <nav className="marketing-nav__links">
          <a href="#why">Why InciTeam</a>
          <a href="#compare">Comparison</a>
          <a href="#workflow">How It Works</a>
        </nav>
        <div className="marketing-nav__actions">
          <Link className="btn btn-outline-primary" to="/signin" {...authLinkProps}>
            Sign In
          </Link>
          <Link className="btn btn-primary" to={primaryPath} {...authLinkProps}>
            {primaryLabel}
          </Link>
        </div>
      </header>

      <main className="marketing-main">
        <section className="marketing-hero">
          <div className="marketing-hero__copy">
            <div className="marketing-tag">Built for ServiceNow-connected incident operations</div>
            <h1>
              Make incident ownership visible, predictable, and easier to trust.
            </h1>
            <p>
              InciTeam helps organizations connect ServiceNow, onboard teams, manage coverage,
              and automate assignment decisions in one focused workspace built for real operational use.
            </p>
            <div className="marketing-hero__actions">
              <Link className="btn btn-primary btn-lg" to={primaryPath} {...authLinkProps}>
                {primaryLabel}
              </Link>
              <Link className="btn btn-outline-primary btn-lg" to={secondaryPath} {...authLinkProps}>
                {secondaryLabel}
              </Link>
            </div>
            <div className="marketing-proof">
              <div>
                <strong>Structured onboarding</strong>
                <span>Create the organization, connect ServiceNow, then invite managers and teams in the right order.</span>
              </div>
              <div>
                <strong>Clear operational visibility</strong>
                <span>Availability, logs, diagnostics, and routing rules stay connected in one workflow.</span>
              </div>
            </div>
          </div>

          <div className="marketing-hero__panel">
            <div className="marketing-console">
              <div className="marketing-console__toolbar">
                <span />
                <span />
                <span />
              </div>
              <div className="marketing-console__title">InciTeam Operations View</div>
              <div className="marketing-console__grid">
                <div className="marketing-console__metric">
                  <div className="marketing-console__label">Routing model</div>
                  <div className="marketing-console__value">CI to owner</div>
                </div>
                <div className="marketing-console__metric">
                  <div className="marketing-console__label">Availability logic</div>
                  <div className="marketing-console__value">Shift-aware</div>
                </div>
                <div className="marketing-console__metric marketing-console__metric--wide">
                  <div className="marketing-console__label">What teams can do inside InciTeam</div>
                  <ul>
                    <li>Connect ServiceNow before setup to avoid broken downstream configuration</li>
                    <li>Manage schedules, leaves, and breaks with assignment awareness</li>
                    <li>See operational logs and diagnostics when a routing decision needs explanation</li>
                    <li>Invite admins, managers, and team members into a shared org model</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="marketing-section" id="why">
          <div className="marketing-section__heading">
            <div className="marketing-section__eyebrow">Key Features</div>
            <h2>Everything teams need to understand coverage, automate assignment, and explain decisions.</h2>
            <p>
              InciTeam is designed to reduce manual coordination during incident operations by connecting ownership,
              schedules, availability, and decision insight in one place.
            </p>
          </div>
          <div className="marketing-pillars">
            {featureHighlights.map((pillar) => (
              <article key={pillar.title} className="marketing-pillar">
                <div className="marketing-pillar__eyebrow">{pillar.eyebrow}</div>
                <h3>{pillar.title}</h3>
                <p>{pillar.description}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="marketing-section" id="compare">
          <div className="marketing-section__heading">
            <div className="marketing-section__eyebrow">Comparison</div>
            <h2>InciTeam is designed for focused incident-team execution, not generic platform sprawl.</h2>
            <p>
              This is a positioning view, not a blanket claim that other platforms lack these capabilities.
              The difference is how directly InciTeam packages them for ServiceNow-connected team operations.
            </p>
          </div>
          <div className="marketing-compare">
            <div className="marketing-compare__header marketing-compare__row">
              <div>Capability</div>
              <div>InciTeam</div>
              <div>Traditional incident suites</div>
            </div>
            {comparisonRows.map((row) => (
              <div key={row.category} className="marketing-compare__row">
                <div className="marketing-compare__category" data-label="Capability">{row.category}</div>
                <div className="marketing-compare__incteam" data-label="InciTeam">{row.incteam}</div>
                <div data-label="Traditional incident suites">{row.traditional}</div>
              </div>
            ))}
          </div>
        </section>

        <section className="marketing-section marketing-section--workflow" id="workflow">
          <div className="marketing-section__heading">
            <div className="marketing-section__eyebrow">How It Works</div>
            <h2>Launch the organization, connect ServiceNow, invite managers, and scale team by team.</h2>
          </div>
          <div className="marketing-workflow">
            <div className="marketing-workflow__step">
              <span>01</span>
              <h3>Create the organization</h3>
              <p>Start with organization name and work email domain so the workspace is anchored to a real company context.</p>
            </div>
            <div className="marketing-workflow__step">
              <span>02</span>
              <h3>Connect ServiceNow first</h3>
              <p>Validate the instance URL and credentials before the rest of setup, so every team builds on a working integration.</p>
            </div>
            <div className="marketing-workflow__step">
              <span>03</span>
              <h3>Invite managers and members</h3>
              <p>Admins can share team access early, letting managers help complete setup and onboard their people in parallel.</p>
            </div>
            <div className="marketing-workflow__step">
              <span>04</span>
              <h3>Operate with clarity</h3>
              <p>Use coverage, logs, diagnostics, mappings, leaves, and breaks from one unified operational surface.</p>
            </div>
          </div>
        </section>

        <section className="marketing-cta">
          <div>
            <div className="marketing-section__eyebrow">Ready To Launch</div>
            <h2>Open the product, sign in, and build the incident workspace around your real teams.</h2>
          </div>
          <div className="marketing-cta__actions">
            <Link className="btn btn-primary btn-lg" to={primaryPath} {...authLinkProps}>
              {primaryLabel}
            </Link>
            <Link className="btn btn-outline-primary btn-lg" to="/signin" {...authLinkProps}>
              Sign In
            </Link>
          </div>
        </section>
      </main>

      <footer className="legal-footer legal-footer--marketing">
        <div className="legal-footer__brand">InciTeam™</div>
        <div>Copyright © 2026 Parth Sarvan. All Rights Reserved.</div>
        <div>U.S. Trademark Application Serial No. 99808275.</div>
        <div>U.S. Copyright Case No. 1-15157770821 pending.</div>
      </footer>
    </div>
  );
}
