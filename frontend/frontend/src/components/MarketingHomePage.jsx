import React from 'react';
import { Link } from 'react-router-dom';
import { getCurrentUser } from '../services/auth';
import { getDefaultRouteForUser } from '../services/permissions';
import './MarketingHomePage.css';

const featureHighlights = [
  {
    eyebrow: 'Organization and Team Management',
    title: 'Model real organizations with multiple teams.',
    description:
      'Create an organization, add teams inside it, and manage each team’s ServiceNow setup, schedules, CI ownership, and incident routing independently.',
  },
  {
    eyebrow: 'Multi-Team User Access',
    title: 'Manage users across teams without duplicate accounts.',
    description:
      'Invite users into the organization and assign them to one or more teams with the right access level. Org admins can move people across teams as responsibilities change.',
  },
  {
    eyebrow: 'CI Ownership Mapping',
    title: 'Route incidents to the right owners in the right order.',
    description:
      'Map each configuration item to the users who support it. Add multiple owners to one CI and arrange them in the exact order InciTeam should follow for round-robin assignment.',
  },
  {
    eyebrow: 'Geo, Shift, and Schedule Coverage',
    title: 'Build 24-hour coverage across teams and regions.',
    description:
      'Create geos, shifts, and user schedules so incidents route only to people actually scheduled for the active geo and shift.',
  },
  {
    eyebrow: 'Roster View',
    title: 'See who is on shift daily or weekly.',
    description:
      'InciTeam automatically generates a roster from schedules, leaves, and breaks so teams can quickly understand current and upcoming coverage.',
  },
  {
    eyebrow: 'Leaves, Breaks, and Handoff Awareness',
    title: 'Keep work visible when someone is unavailable.',
    description:
      'Record planned leaves and short breaks with date/time ranges. InciTeam avoids unavailable users during assignment and highlights active incidents sitting with someone who is away.',
  },
  {
    eyebrow: 'Smart Incident Assignment',
    title: 'Check ownership, availability, and critical workload before assigning.',
    description:
      'Before assigning, InciTeam checks CI ownership, active geo, active shift, schedules, breaks, leaves, and whether the selected user is already handling P0/P1C work.',
  },
  {
    eyebrow: 'Round-Robin Workload Distribution',
    title: 'Distribute work fairly across mapped owners.',
    description:
      'For each CI, InciTeam distributes incidents across mapped owners in configured order, reducing manual triage and preventing one person from becoming the default owner.',
  },
  {
    eyebrow: 'Automatic and Manual Polling',
    title: 'Catch eligible incidents quickly.',
    description:
      'InciTeam polls ServiceNow automatically on a fixed schedule and also provides a manual Poll Now option when teams need an immediate refresh, helping protect SLA response windows.',
  },
  {
    eyebrow: 'Operational Logs',
    title: 'Understand what happened to every incident.',
    description:
      'Every poll, selection, skip, failure, and assignment is recorded so managers can see exactly what happened and why.',
  },
  {
    eyebrow: 'Diagnostics and Summary',
    title: 'Validate setup and focus attention fast.',
    description:
      'Dry-run diagnostics explain assignment decisions, while Summary highlights connection health, latest poll results, validation risks, coverage gaps, and handoff items.',
  },
];

export default function MarketingHomePage() {
  const currentUser = getCurrentUser();
  const primaryPath = currentUser ? getDefaultRouteForUser(currentUser) : '/signup';
  const primaryLabel = currentUser ? 'Open App' : 'Create Organization';
  const authLinkProps = currentUser ? {} : { target: '_blank', rel: 'noreferrer' };

  return (
    <div className="marketing-page">
      <header className="marketing-nav">
        <div className="marketing-nav__brand">
          <div className="marketing-nav__eyebrow">Incident and Team Management Platform for Service Now</div>
          <div className="marketing-nav__name">InciTeam</div>
        </div>
        <nav className="marketing-nav__links">
          <a href="#why">Features</a>
          <a href="#workflow">How It Works</a>
        </nav>
      </header>

      <main className="marketing-main">
        <section className="marketing-hero">
          <div className="marketing-hero__copy">
            <div className="marketing-tag">Built for ServiceNow-connected incident operations</div>
            <h1>
              Automate ServiceNow incident assignment with team-aware routing.
            </h1>
            <p>
              InciTeam helps organizations create teams, map CIs to owners, manage schedules,
              leaves, and breaks, then assign incidents through explainable round-robin logic.
            </p>
            <div className="marketing-proof">
              <div>
                <strong>No manual triage</strong>
                <span>Route incidents using CI ownership, active shift, leaves, breaks, and P0/P1C workload checks.</span>
              </div>
              <div>
                <strong>Built for teams</strong>
                <span>Create organizations, manage teams, assign users across teams, and keep routing rules separate.</span>
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
              <div className="marketing-decision-card">
                <div className="marketing-console__label">Incident received</div>
                <div className="marketing-decision-card__incident">INC0010042</div>
                <div className="marketing-decision-card__meta">
                  <span>CI: Sales Force Automation</span>
                  <span>Group: Software</span>
                </div>
              </div>
              <div className="marketing-decision-flow">
                {[
                  'CI owner list found',
                  'Active geo and shift checked',
                  'Leave and break checked',
                  'P0/P1C workload checked',
                  'Assigned by round-robin',
                ].map((step, index) => (
                  <div className="marketing-decision-step" key={step}>
                    <span>{index + 1}</span>
                    <strong>{step}</strong>
                  </div>
                ))}
              </div>
              <div className="marketing-decision-result">
                <div>
                  <div className="marketing-console__label">Result</div>
                  <strong>Assigned to Ava King</strong>
                  <span>Decision logged for review</span>
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
